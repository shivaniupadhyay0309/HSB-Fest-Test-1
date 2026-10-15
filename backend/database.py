import os
import shutil
import sqlite3
from pathlib import Path

# Anchor DB path to the project root directory
BASE_DIR = Path(__file__).resolve().parent.parent

# In serverless environments like Vercel, the code root is read-only.
# We copy fest.db to /tmp so SQLite write operations (registrations, add/delete event) succeed.
if os.environ.get("VERCEL"):
    DB_PATH = Path("/tmp/fest.db")
    BUNDLED_DB = BASE_DIR / "fest.db"
    if not DB_PATH.exists() and BUNDLED_DB.exists():
        try:
            shutil.copy2(BUNDLED_DB, DB_PATH)
        except Exception as e:
            print(f"Vercel DB copy notice: {e}")
else:
    DB_PATH = BASE_DIR / "fest.db"

def get_db_connection():
    """
    Returns an open SQLite database connection with row_factory set to sqlite3.Row
    and foreign key constraints enabled.
    """
    conn = sqlite3.connect(str(DB_PATH))
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = ON;")
    return conn

def init_db():
    """
    Creates the 'events' and 'registrations' tables if they do not exist.
    """
    conn = get_db_connection()
    cursor = conn.cursor()

    cursor.execute("""
    CREATE TABLE IF NOT EXISTS events (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        title TEXT NOT NULL,
        category TEXT NOT NULL,
        description TEXT,
        event_date TEXT NOT NULL,
        event_time TEXT NOT NULL,
        venue TEXT NOT NULL,
        capacity INTEGER NOT NULL,
        fee INTEGER NOT NULL DEFAULT 0,
        icon TEXT
    );
    """)

    cursor.execute("""
    CREATE TABLE IF NOT EXISTS registrations (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        event_id INTEGER NOT NULL REFERENCES events(id),
        student_name TEXT NOT NULL,
        roll_number TEXT NOT NULL,
        email TEXT NOT NULL,
        phone TEXT,
        registered_at TEXT NOT NULL,
        UNIQUE(event_id, email)
    );
    """)

    conn.commit()
    conn.close()

if __name__ == "__main__":
    init_db()
    print("Database initialized successfully at", DB_PATH)
