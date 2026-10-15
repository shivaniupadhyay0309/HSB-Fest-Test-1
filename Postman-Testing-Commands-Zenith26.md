# Postman Testing Commands & API Contract Guide — Zenith '26 Fest System

This guide outlines all **18 API test cases** to verify the Zenith '26 Fest Management System backend endpoints, status codes, request bodies, and expected responses.

---

## 🛠️ Environment Setup

- **Base URL:** `http://localhost:8001`
- **FastAPI Interactive Docs:** `http://localhost:8001/docs`
- **FastAPI OpenAPI Schema:** `http://localhost:8001/openapi.json`

To import into Postman:
1. Open Postman desktop or web client.
2. Click **Import** (top left).
3. Select the file `zenith26-api.postman_collection.json` located at the root of `zenith26-fest-system`.
4. Ensure your server is running via `uvicorn backend.main:app --reload`.

---

## 📋 Comprehensive 18-Point Test Suite

### 1. Events Endpoints (`/api/events`)

#### Test 1: Get All Events
- **Method:** `GET`
- **URL:** `http://localhost:8001/api/events`
- **Expected Status:** `200 OK`
- **Verification:** Returns array of 14 events. Each event contains `id`, `title`, `category`, `capacity`, `fee`, `available_seats`, etc.

#### Test 2: Filter by Category (Technical)
- **Method:** `GET`
- **URL:** `http://localhost:8001/api/events?category=Technical`
- **Expected Status:** `200 OK`
- **Verification:** Returns only Technical events (HackZenith, RoboWars, CodeSprint).

#### Test 3: Filter by Category (Cultural)
- **Method:** `GET`
- **URL:** `http://localhost:8001/api/events?category=Cultural`
- **Expected Status:** `200 OK`
- **Verification:** Returns Cultural events (Zenith Idol, Battle of the Bands, Step Up).

#### Test 4: Search by Title (Case-Insensitive)
- **Method:** `GET`
- **URL:** `http://localhost:8001/api/events?search=hack`
- **Expected Status:** `200 OK`
- **Verification:** Returns "HackZenith 24hr Hackathon".

#### Test 5: Combined Filter and Search
- **Method:** `GET`
- **URL:** `http://localhost:8001/api/events?category=Gaming&search=valorant`
- **Expected Status:** `200 OK`
- **Verification:** Returns "Valorant Campus Championship".

#### Test 6: Search with No Matches (Empty State)
- **Method:** `GET`
- **URL:** `http://localhost:8001/api/events?search=nonexistentterm123`
- **Expected Status:** `200 OK`
- **Verification:** Returns empty JSON array `[]`.

---

### 2. Event Detail Endpoints (`/api/events/{event_id}`)

#### Test 7: Get Existing Event Detail
- **Method:** `GET`
- **URL:** `http://localhost:8001/api/events/1`
- **Expected Status:** `200 OK`
- **Verification:** Returns single event object for ID 1.

#### Test 8: Get Non-Existent Event Detail (404)
- **Method:** `GET`
- **URL:** `http://localhost:8001/api/events/999`
- **Expected Status:** `404 Not Found`
- **Expected Response Body:**
  ```json
  {
    "detail": "Event with ID 999 not found"
  }
  ```

---

### 3. Registration Endpoints (`/api/events/{event_id}/register`)

#### Test 9: Successful Student Registration (201)
- **Method:** `POST`
- **URL:** `http://localhost:8001/api/events/1/register`
- **Header:** `Content-Type: application/json`
- **Body:**
  ```json
  {
    "student_name": "Aarav Sharma",
    "roll_number": "26CS101",
    "email": "aarav.sharma@college.edu",
    "phone": "9876543210"
  }
  ```
- **Expected Status:** `201 Created`
- **Expected Response Body:**
  ```json
  {
    "id": 1,
    "event_id": 1,
    "student_name": "Aarav Sharma",
    "registered_at": "..."
  }
  ```

#### Test 10: Validation Error — Missing Name (400)
- **Method:** `POST`
- **URL:** `http://localhost:8001/api/events/1/register`
- **Body:**
  ```json
  {
    "student_name": "",
    "roll_number": "26CS102",
    "email": "student@college.edu"
  }
  ```
- **Expected Status:** `400 Bad Request`
- **Expected Response Detail:** `"Student name, roll number, and email must not be empty"`

#### Test 11: Validation Error — Missing Roll Number (400)
- **Method:** `POST`
- **URL:** `http://localhost:8001/api/events/1/register`
- **Body:**
  ```json
  {
    "student_name": "Rohan Gupta",
    "roll_number": "",
    "email": "rohan@college.edu"
  }
  ```
- **Expected Status:** `400 Bad Request`

#### Test 12: Validation Error — Invalid Email Missing '@' (400)
- **Method:** `POST`
- **URL:** `http://localhost:8001/api/events/1/register`
- **Body:**
  ```json
  {
    "student_name": "Priya Singh",
    "roll_number": "26EE104",
    "email": "priyasingh.college.edu"
  }
  ```
- **Expected Status:** `400 Bad Request`
- **Expected Response Detail:** `"Invalid email: Email must contain '@'"`

#### Test 13: Duplicate Registration Conflict (409)
- **Method:** `POST`
- **URL:** `http://localhost:8001/api/events/1/register`
- **Body:**
  ```json
  {
    "student_name": "Aarav Sharma",
    "roll_number": "26CS101",
    "email": "aarav.sharma@college.edu"
  }
  ```
- **Expected Status:** `409 Conflict`
- **Expected Response Detail:** `"Already registered"`

#### Test 14: Register for Non-Existent Event (404)
- **Method:** `POST`
- **URL:** `http://localhost:8001/api/events/999/register`
- **Body:**
  ```json
  {
    "student_name": "Test User",
    "roll_number": "26ME105",
    "email": "test@college.edu"
  }
  ```
- **Expected Status:** `404 Not Found`

---

### 4. Registrations List & Stats

#### Test 15: Get All Registrations
- **Method:** `GET`
- **URL:** `http://localhost:8001/api/registrations`
- **Expected Status:** `200 OK`
- **Verification:** List includes student details alongside `event_title`.

#### Test 16: Get Registrations Filtered by Event ID
- **Method:** `GET`
- **URL:** `http://localhost:8001/api/registrations?event_id=1`
- **Expected Status:** `200 OK`
- **Verification:** Only registrations for Event 1 are returned.

#### Test 17: Get Operations Stats
- **Method:** `GET`
- **URL:** `http://localhost:8001/api/stats`
- **Expected Status:** `200 OK`
- **Verification:** Response contains live metrics:
  ```json
  {
    "total_events": 14,
    "total_registrations": 1,
    "total_capacity": 1509,
    "overall_utilization_pct": 0.1,
    "upcoming_events_count": 14,
    "events_by_category": {
      "Cultural": 3,
      "Gaming": 3,
      "Sports": 3,
      "Technical": 3,
      "Workshop": 2
    }
  }
  ```

---

### 5. Stretch Goals (Admin Operations)

#### Test 18: Admin Add Event (201 Created)
- **Method:** `POST`
- **URL:** `http://localhost:8001/api/events`
- **Header:** `Content-Type: application/json`
- **Body:**
  ```json
  {
    "title": "Quantum Computing Primer",
    "category": "Workshop",
    "description": "An introduction to quantum gates and algorithms using Qiskit.",
    "event_date": "2026-11-15",
    "event_time": "03:00 PM",
    "venue": "Physics Lab 4",
    "capacity": 40,
    "fee": 0,
    "icon": "⚛️"
  }
  ```
- **Expected Status:** `201 Created`
- **Verification:** Returns created event with assigned `id`.
