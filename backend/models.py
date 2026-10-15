from typing import Optional, Dict
from pydantic import BaseModel, Field

class EventOut(BaseModel):
    id: int
    title: str
    category: str
    description: Optional[str] = None
    event_date: str
    event_time: str
    venue: str
    capacity: int
    fee: int = 0
    icon: Optional[str] = None
    available_seats: int

class EventDetail(EventOut):
    pass

class EventCreate(BaseModel):
    title: str = Field(..., min_length=1, description="Event title")
    category: str = Field(..., min_length=1, description="Category (e.g. Technical, Cultural)")
    description: Optional[str] = None
    event_date: str = Field(..., min_length=1, description="Date in YYYY-MM-DD format")
    event_time: str = Field(..., min_length=1, description="Time in HH:MM AM/PM format")
    venue: str = Field(..., min_length=1, description="Event venue")
    capacity: int = Field(..., gt=0, description="Total seating capacity (must be > 0)")
    fee: int = Field(0, ge=0, description="Registration fee in INR (>= 0)")
    icon: Optional[str] = Field("✨", description="Emoji icon representation")

class RegistrationIn(BaseModel):
    student_name: Optional[str] = Field(default="", description="Student full name")
    roll_number: Optional[str] = Field(default="", description="Student roll number / ID")
    email: Optional[str] = Field(default="", description="Student email address containing @")
    phone: Optional[str] = Field(default=None, description="Optional phone number")

class RegistrationOut(BaseModel):
    id: int
    event_id: int
    student_name: str
    registered_at: str

class RegistrationItemOut(BaseModel):
    id: int
    event_id: int
    event_title: str
    student_name: str
    roll_number: str
    email: str
    phone: Optional[str] = None
    registered_at: str

class StatsOut(BaseModel):
    total_events: int
    total_registrations: int
    total_capacity: int
    overall_utilization_pct: float
    upcoming_events_count: int
    events_by_category: Dict[str, int]
