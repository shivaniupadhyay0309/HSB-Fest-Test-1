from fastapi.testclient import TestClient
from backend.main import app

client = TestClient(app)

def run_tests():
    print("--- Running Zenith '26 API Test Suite ---")

    # 1. GET /api/events
    res = client.get("/api/events")
    assert res.status_code == 200, f"Expected 200, got {res.status_code}"
    events = res.json()
    print(f"[PASS] GET /api/events -> {len(events)} events returned.")
    assert len(events) >= 14, f"Expected 14 events, got {len(events)}"

    # 2. Filter Category & Search
    res = client.get("/api/events?category=Technical")
    assert res.status_code == 200
    tech_events = res.json()
    print(f"[PASS] GET /api/events?category=Technical -> {len(tech_events)} events returned.")

    res = client.get("/api/events?search=hack")
    assert res.status_code == 200
    search_events = res.json()
    title = search_events[0]["title"]
    print(f"[PASS] GET /api/events?search=hack -> found: {title}")

    # 3. GET /api/events/{id}
    res = client.get("/api/events/1")
    assert res.status_code == 200
    print(f"[PASS] GET /api/events/1 -> {res.json()['title']}")

    res = client.get("/api/events/999")
    assert res.status_code == 404
    print(f"[PASS] GET /api/events/999 -> 404: {res.json()['detail']}")

    import uuid
    unique_suffix = uuid.uuid4().hex[:6]
    test_email = f"student_{unique_suffix}@college.edu"

    # 4. POST /api/events/1/register (Success)
    payload = {
        "student_name": "Shivani Upadhyay",
        "roll_number": "26MBA104",
        "email": test_email,
        "phone": "9876543210"
    }
    res = client.post("/api/events/1/register", json=payload)
    assert res.status_code == 201, f"Expected 201, got {res.status_code}: {res.text}"
    reg_data = res.json()
    print(f"[PASS] POST /api/events/1/register -> 201 Created (ID #{reg_data['id']})")

    # 5. POST duplicate registration (409 Conflict)
    res = client.post("/api/events/1/register", json=payload)
    assert res.status_code == 409
    print(f"[PASS] Duplicate registration -> 409 Conflict ({res.json()['detail']})")

    # 6. POST missing field (400 Bad Request)
    bad_payload = {"student_name": "", "roll_number": "26MBA104", "email": "shivani@college.edu"}
    res = client.post("/api/events/1/register", json=bad_payload)
    assert res.status_code == 400
    print(f"[PASS] Missing student_name -> 400 Bad Request ({res.json()['detail']})")

    # 7. POST invalid email without @ (400 Bad Request)
    bad_email = {"student_name": "Test Student", "roll_number": "26MBA105", "email": "invalid_email.edu"}
    res = client.post("/api/events/1/register", json=bad_email)
    assert res.status_code == 400
    print(f"[PASS] Invalid email without @ -> 400 Bad Request ({res.json()['detail']})")

    # 8. GET /api/registrations
    res = client.get("/api/registrations")
    assert res.status_code == 200
    regs = res.json()
    print(f"[PASS] GET /api/registrations -> {len(regs)} registrations. Event: {regs[0]['event_title']}")

    # 9. GET /api/stats
    res = client.get("/api/stats")
    assert res.status_code == 200
    stats = res.json()
    print(f"[PASS] GET /api/stats -> Events: {stats['total_events']}, Registrations: {stats['total_registrations']}, Util: {stats['overall_utilization_pct']}%, Upcoming: {stats['upcoming_events_count']}")

    # 10. Frontend static serving
    res = client.get("/")
    assert res.status_code == 200
    assert "Zenith '26" in res.text
    print(f"[PASS] GET / -> served index.html cleanly ({len(res.text)} bytes)")

    res = client.get("/admin.html")
    assert res.status_code == 200
    assert "Fest Operations & Analytics" in res.text
    print(f"[PASS] GET /admin.html -> served admin.html cleanly ({len(res.text)} bytes)")

    res = client.get("/js/app.js")
    assert res.status_code == 200
    print(f"[PASS] GET /js/app.js -> served app.js ({len(res.text)} bytes)")

    # 11. Stretch Goal: Admin Cancel Registration
    res = client.delete(f"/api/registrations/{reg_data['id']}")
    assert res.status_code == 200
    print(f"[PASS] DELETE /api/registrations/{reg_data['id']} -> 200 OK (Registration cancelled, seat released)")

    print("\n✅ ALL API CONTRACT TESTS PASSED PERFECTLY!")

if __name__ == "__main__":
    run_tests()
