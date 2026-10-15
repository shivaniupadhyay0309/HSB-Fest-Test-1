from datetime import datetime, date
from pathlib import Path
from typing import Optional, List
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException, Query, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from fastapi.responses import FileResponse

from backend.database import get_db_connection, init_db
from backend.seed_data import seed_events
from backend.models import (
    EventOut,
    EventDetail,
    EventCreate,
    RegistrationIn,
    RegistrationOut,
    RegistrationItemOut,
    StatsOut,
)

BASE_DIR = Path(__file__).resolve().parent.parent
FRONTEND_DIR = BASE_DIR / "frontend"

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Initialize DB and seed events on startup
    init_db()
    try:
        seed_events()
    except Exception as e:
        print(f"Startup seed notice: {e}")
    yield

from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

app = FastAPI(
    title="Zenith '26 College Fest Management System API",
    description="Backend API for managing events, registrations, and fest analytics.",
    version="1.0.0",
    lifespan=lifespan,
)

@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request, exc):
    return JSONResponse(
        status_code=status.HTTP_400_BAD_REQUEST,
        content={"detail": "Invalid or missing request fields in payload"},
    )

# Enable CORS for local development
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ---------------------------------------------------------
# API Endpoints
# ---------------------------------------------------------

@app.get("/api/events", response_model=List[EventOut], tags=["Events"])
def get_events(
    category: Optional[str] = Query(None, description="Filter by event category"),
    search: Optional[str] = Query(None, description="Search by title (case-insensitive)"),
):
    """
    Returns list of events with available seats calculated dynamically.
    Supports filtering by category and case-insensitive search by title.
    """
    conn = get_db_connection()
    query = """
        SELECT e.id, e.title, e.category, e.description, e.event_date,
               e.event_time, e.venue, e.capacity, e.fee, e.icon,
               (e.capacity - COALESCE(reg_counts.cnt, 0)) AS available_seats
        FROM events e
        LEFT JOIN (
            SELECT event_id, COUNT(*) AS cnt
            FROM registrations
            GROUP BY event_id
        ) reg_counts ON e.id = reg_counts.event_id
        WHERE 1=1
    """
    params = []

    if category and category.strip() and category.strip().lower() != "all":
        query += " AND LOWER(e.category) = LOWER(?)"
        params.append(category.strip())

    if search and search.strip():
        query += " AND LOWER(e.title) LIKE LOWER(?)"
        params.append(f"%{search.strip()}%")

    query += " ORDER BY e.event_date ASC, e.event_time ASC, e.id ASC"

    rows = conn.execute(query, params).fetchall()
    conn.close()

    events = []
    for r in rows:
        events.append(
            EventOut(
                id=r["id"],
                title=r["title"],
                category=r["category"],
                description=r["description"],
                event_date=r["event_date"],
                event_time=r["event_time"],
                venue=r["venue"],
                capacity=r["capacity"],
                fee=r["fee"],
                icon=r["icon"],
                available_seats=max(0, int(r["available_seats"])),
            )
        )
    return events


@app.get("/api/events/{event_id}", response_model=EventDetail, tags=["Events"])
def get_event_detail(event_id: int):
    """
    Returns single event details or 404 if not found.
    """
    conn = get_db_connection()
    query = """
        SELECT e.id, e.title, e.category, e.description, e.event_date,
               e.event_time, e.venue, e.capacity, e.fee, e.icon,
               (e.capacity - COALESCE(reg_counts.cnt, 0)) AS available_seats
        FROM events e
        LEFT JOIN (
            SELECT event_id, COUNT(*) AS cnt
            FROM registrations
            GROUP BY event_id
        ) reg_counts ON e.id = reg_counts.event_id
        WHERE e.id = ?
    """
    row = conn.execute(query, (event_id,)).fetchone()
    conn.close()

    if not row:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Event with ID {event_id} not found",
        )

    return EventDetail(
        id=row["id"],
        title=row["title"],
        category=row["category"],
        description=row["description"],
        event_date=row["event_date"],
        event_time=row["event_time"],
        venue=row["venue"],
        capacity=row["capacity"],
        fee=row["fee"],
        icon=row["icon"],
        available_seats=max(0, int(row["available_seats"])),
    )


@app.post(
    "/api/events/{event_id}/register",
    response_model=RegistrationOut,
    status_code=status.HTTP_201_CREATED,
    tags=["Registrations"],
)
def register_for_event(event_id: int, payload: RegistrationIn):
    """
    Registers a student for an event with strict server-side validation.
    """
    name = payload.student_name.strip() if payload.student_name else ""
    roll = payload.roll_number.strip() if payload.roll_number else ""
    email = payload.email.strip().lower() if payload.email else ""
    phone = payload.phone.strip() if payload.phone else None

    # Server-side validation
    if not name or not roll or not email:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Student name, roll number, and email must not be empty",
        )

    if "@" not in email:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Invalid email: Email must contain '@'",
        )

    conn = get_db_connection()
    cursor = conn.cursor()

    # 1. Event must exist
    event = cursor.execute("SELECT id, title, capacity FROM events WHERE id = ?", (event_id,)).fetchone()
    if not event:
        conn.close()
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Event with ID {event_id} not found",
        )

    # 2. Check if already registered
    existing = cursor.execute(
        "SELECT id FROM registrations WHERE event_id = ? AND LOWER(email) = ?",
        (event_id, email),
    ).fetchone()
    if existing:
        conn.close()
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Already registered",
        )

    # 3. Check capacity
    reg_count = cursor.execute(
        "SELECT COUNT(*) AS cnt FROM registrations WHERE event_id = ?",
        (event_id,),
    ).fetchone()["cnt"]

    if reg_count >= event["capacity"]:
        conn.close()
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Event is full",
        )

    # On success: insert registration
    now_str = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    cursor.execute("""
        INSERT INTO registrations (event_id, student_name, roll_number, email, phone, registered_at)
        VALUES (?, ?, ?, ?, ?, ?)
    """, (event_id, name, roll, email, phone, now_str))

    reg_id = cursor.lastrowid
    conn.commit()
    conn.close()

    return RegistrationOut(
        id=reg_id,
        event_id=event_id,
        student_name=name,
        registered_at=now_str,
    )


@app.get("/api/registrations", response_model=List[RegistrationItemOut], tags=["Registrations"])
def get_registrations(event_id: Optional[int] = Query(None, description="Filter registrations by event ID")):
    """
    Returns list of registrations, each including event title for admin dashboard.
    """
    conn = get_db_connection()
    query = """
        SELECT r.id, r.event_id, e.title AS event_title, r.student_name,
               r.roll_number, r.email, r.phone, r.registered_at
        FROM registrations r
        JOIN events e ON r.event_id = e.id
        WHERE 1=1
    """
    params = []
    if event_id is not None:
        query += " AND r.event_id = ?"
        params.append(event_id)

    query += " ORDER BY r.registered_at DESC, r.id DESC"

    rows = conn.execute(query, params).fetchall()
    conn.close()

    result = []
    for r in rows:
        result.append(
            RegistrationItemOut(
                id=r["id"],
                event_id=r["event_id"],
                event_title=r["event_title"],
                student_name=r["student_name"],
                roll_number=r["roll_number"],
                email=r["email"],
                phone=r["phone"],
                registered_at=r["registered_at"],
            )
        )
    return result


@app.get("/api/stats", response_model=StatsOut, tags=["Stats"])
def get_stats():
    """
    Returns live statistics calculated on-the-fly from SQLite:
    - total_events
    - total_registrations
    - total_capacity
    - overall_utilization_pct (rounded to 1 decimal)
    - upcoming_events_count (event_date >= today)
    - events_by_category (dict)
    """
    conn = get_db_connection()

    total_events = conn.execute("SELECT COUNT(*) AS cnt FROM events").fetchone()["cnt"]
    total_registrations = conn.execute("SELECT COUNT(*) AS cnt FROM registrations").fetchone()["cnt"]
    total_capacity = conn.execute("SELECT COALESCE(SUM(capacity), 0) AS cap FROM events").fetchone()["cap"]

    overall_utilization_pct = 0.0
    if total_capacity > 0:
        overall_utilization_pct = round((total_registrations / total_capacity) * 100.0, 1)

    today_str = date.today().isoformat()
    upcoming_events_count = conn.execute(
        "SELECT COUNT(*) AS cnt FROM events WHERE event_date >= ?",
        (today_str,),
    ).fetchone()["cnt"]

    category_rows = conn.execute(
        "SELECT category, COUNT(*) AS cnt FROM events GROUP BY category ORDER BY category ASC"
    ).fetchall()

    events_by_category = {row["category"]: row["cnt"] for row in category_rows}

    conn.close()

    return StatsOut(
        total_events=total_events,
        total_registrations=total_registrations,
        total_capacity=total_capacity,
        overall_utilization_pct=overall_utilization_pct,
        upcoming_events_count=upcoming_events_count,
        events_by_category=events_by_category,
    )


# Stretch Goal 1: POST /api/events (Admin Add Event)
@app.post("/api/events", response_model=EventOut, status_code=status.HTTP_201_CREATED, tags=["Admin"])
def create_event(payload: EventCreate):
    """
    Allows admin to add a new event with full server validation.
    """
    title = payload.title.strip()
    category = payload.category.strip()
    venue = payload.venue.strip()

    if not title or not category or not venue:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Title, category, and venue are required",
        )

    if payload.capacity <= 0:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Capacity must be greater than 0",
        )

    if payload.fee < 0:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Fee cannot be negative",
        )

    conn = get_db_connection()
    cursor = conn.cursor()

    cursor.execute("""
        INSERT INTO events (title, category, description, event_date, event_time, venue, capacity, fee, icon)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
    """, (
        title,
        category,
        payload.description.strip() if payload.description else "",
        payload.event_date.strip(),
        payload.event_time.strip(),
        venue,
        payload.capacity,
        payload.fee,
        payload.icon.strip() if payload.icon else "✨"
    ))

    new_id = cursor.lastrowid
    conn.commit()
    conn.close()

    return EventOut(
        id=new_id,
        title=title,
        category=category,
        description=payload.description,
        event_date=payload.event_date,
        event_time=payload.event_time,
        venue=venue,
        capacity=payload.capacity,
        fee=payload.fee,
        icon=payload.icon,
        available_seats=payload.capacity,
    )


# Stretch Goal 2: DELETE /api/registrations/{registration_id} (Admin Cancel Registration)
@app.delete("/api/registrations/{registration_id}", tags=["Admin"])
def cancel_registration(registration_id: int):
    """
    Allows admin to cancel a registration, instantly freeing up the seat.
    """
    conn = get_db_connection()
    cursor = conn.cursor()

    reg = cursor.execute("SELECT id, event_id, student_name FROM registrations WHERE id = ?", (registration_id,)).fetchone()
    if not reg:
        conn.close()
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Registration with ID {registration_id} not found",
        )

    cursor.execute("DELETE FROM registrations WHERE id = ?", (registration_id,))
    conn.commit()
    conn.close()

    return {
        "message": "Registration cancelled successfully",
        "registration_id": registration_id,
        "event_id": reg["event_id"],
        "student_name": reg["student_name"],
    }


# ---------------------------------------------------------
# Static File Serving & Frontend Routes (Step 4)
# ---------------------------------------------------------

if (FRONTEND_DIR / "js").exists():
    app.mount("/js", StaticFiles(directory=str(FRONTEND_DIR / "js")), name="js")

@app.get("/", include_in_schema=False)
def serve_index():
    index_file = FRONTEND_DIR / "index.html"
    if index_file.exists():
        return FileResponse(str(index_file))
    return {"message": "frontend/index.html not found"}

@app.get("/index.html", include_in_schema=False)
def serve_index_html():
    return serve_index()

@app.get("/admin.html", include_in_schema=False)
def serve_admin():
    admin_file = FRONTEND_DIR / "admin.html"
    if admin_file.exists():
        return FileResponse(str(admin_file))
    return {"message": "frontend/admin.html not found"}
