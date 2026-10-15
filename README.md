# Zenith '26 College Fest Management System ⚡

A complete, production-grade college fest management system featuring a **Native Android Application (Kotlin + XML Layouts)** and a **Python + FastAPI Backend** with **SQLite** database persistence.

---

## 🌟 What This Project Is

Zenith '26 is an end-to-end event management application built for college fests:
- **Student Event Portal:** Browse 14 fest events across categories (Technical, Cultural, Sports, Gaming, Workshop), perform real-time search, filter by track, view event details, and register with instant seat allocation.
- **Admin Operations Dashboard:** Live operations telemetry including 4 stat cards (Total Events, Confirmed Registrations, Capacity Utilization %, Upcoming Events), category breakdown distribution, capacity utilization heatmap table (sorted descending with >90% warning alerts), and a complete student registrations ledger with event filters.
- **Stretch Goals Included:**
  - Create new fest events via `POST /api/events`.
  - Cancel existing registrations via `DELETE /api/registrations/{id}` with immediate seat release.

---

## 🛠️ Technology Stack

- **Android Frontend:**
  - Language: **Kotlin**
  - UI: **Native Android XML Layouts** + Material Components 3
  - Networking: **Retrofit 2** + **OkHttp 3** (with logging interceptor)
  - Serialization: **Gson**
  - Concurrency: **Kotlin Coroutines** (`Dispatchers.IO`)
  - Target SDK: 34 (Android 14) | Min SDK: 24 (Android 7.0+)
- **Backend:**
  - Framework: **Python 3.10+ / 3.14** + **FastAPI**
  - Server: **Uvicorn**
  - Data Validation: **Pydantic v2**
  - Database: **SQLite 3** (`fest.db`)
  - Seed File Ingestion: **openpyxl** reading `data/events_seed.xlsx`
- **Testing:**
  - **Postman:** `zenith26-api.postman_collection.json`
  - Automated Python Suite: `test_suite.py`

---

## 📁 Project Structure

```text
zenith26-fest-system/
├── backend/
│   ├── database.py       # SQLite connection helper & schema creation
│   ├── main.py           # FastAPI endpoints, CORS, validation, lifespan
│   ├── models.py         # Pydantic models (EventOut, RegistrationIn, StatsOut, etc.)
│   └── seed_data.py      # Spreadsheet ingestion from data/events_seed.xlsx
├── android/
│   ├── app/
│   │   ├── src/
│   │   │   └── main/
│   │   │       ├── java/com/zenith26/fest/
│   │   │       │   ├── MainActivity.kt
│   │   │       │   ├── api/
│   │   │       │   │   ├── ApiClient.kt
│   │   │       │   │   └── ApiService.kt
│   │   │       │   ├── models/
│   │   │       │   │   ├── Event.kt
│   │   │       │   │   ├── Registration.kt
│   │   │       │   │   ├── Stats.kt
│   │   │       │   │   └── ApiErrorResponse.kt
│   │   │       │   ├── screens/
│   │   │       │   │   ├── StudentPortalFragment.kt
│   │   │       │   │   ├── AdminDashboardFragment.kt
│   │   │       │   │   ├── EventDetailDialogFragment.kt
│   │   │       │   │   ├── AddEventDialogFragment.kt
│   │   │       │   │   └── SettingsDialogFragment.kt
│   │   │       │   ├── adapters/
│   │   │       │   │   ├── EventAdapter.kt
│   │   │       │   │   ├── AdminEventAdapter.kt
│   │   │       │   │   ├── RegistrationAdapter.kt
│   │   │       │   │   └── CategoryBreakdownAdapter.kt
│   │   │       │   └── utils/
│   │   │       │       ├── CategoryThemeHelper.kt
│   │   │       │       └── NetworkUtils.kt
│   │   │       ├── res/
│   │   │       │   ├── anim/fade_in.xml
│   │   │       │   ├── drawable/
│   │   │       │   ├── layout/
│   │   │       │   ├── mipmap-anydpi-v26/
│   │   │       │   └── values/
│   │   │       └── AndroidManifest.xml
│   │   └── build.gradle.kts
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   └── gradle.properties
├── data/
│   └── events_seed.xlsx  # 14 fest events companion spreadsheet
├── requirements.txt
├── zenith26-api.postman_collection.json
├── Postman-Testing-Commands-Zenith26.md
├── test_suite.py
└── README.md
```

---

## 🚀 How to Run the Python Backend

### 1. Open Terminal in `zenith26-fest-system`
Navigate into the project root directory:
```bash
cd zenith26-fest-system
```

### 2. Activate Virtual Environment (or create one)
```powershell
# Windows PowerShell:
.\venv\Scripts\Activate.ps1
```
*(If setting up a fresh virtual environment: `python -m venv venv` followed by `pip install -r requirements.txt`)*

### 3. Verify Database Seed (Optional)
```bash
python -m backend.seed_data
```
Outputs the 14 events loaded from `data/events_seed.xlsx`.

### 4. Start the Servers

#### A. Web Fest System (Port 8080)
```bash
uvicorn backend.main:app --reload --port 8080
```
- Student Web Portal: `http://localhost:8080/`
- Admin Web Dashboard: `http://localhost:8080/admin.html`
- Interactive API Docs: `http://localhost:8080/docs`

#### B. Mobile App Backend (Port 8090)
```bash
uvicorn backend.main:app --reload --port 8090
```
- API Root: `http://localhost:8090`
- Interactive API Docs: `http://localhost:8090/docs`

---

## 📱 How to Open, Build, and Run the Android Application

### 1. Open Android Studio
1. Launch **Android Studio**.
2. Select **Open** and choose the `android` directory:
   `zenith26-fest-system/android`
3. Allow Gradle to sync dependencies.

### 2. Connect the Android Emulator to the Local Backend
- By default, Android Emulators run inside a virtual network. To access `localhost` of your development computer from inside the emulator, use the special loopback alias:
  ```text
  http://10.0.2.2:8090/
  ```
- **Built-in Configuration UI:** If testing on a physical device over Wi-Fi, tap the **Settings Gear icon** in the top-right of the app's navbar and enter your machine's local IP (e.g. `http://192.168.1.50:8090/`).
- `AndroidManifest.xml` is pre-configured with `android:usesCleartextTraffic="true"` to support local HTTP communication.

### 3. Run the App
- Select your target device or emulator in Android Studio.
- Press **Run 'app'** (`Shift + F10`).
- The app will open displaying the **Zenith '26 Student Portal** with all 14 events fetched from the live backend.

---

## 🔌 API Endpoints Contract

| Method | Endpoint | Description | Status Code |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/events` | List all events (supports `?category=` and `?search=`) with dynamic seat count | `200 OK` |
| `GET` | `/api/events/{id}` | Retrieve details for a specific event | `200 OK` / `404 Not Found` |
| `POST` | `/api/events/{id}/register` | Register student for an event (checks duplicate email & capacity) | `201 Created` / `400` / `409` |
| `GET` | `/api/registrations` | View registrations list (supports optional `?event_id=`) | `200 OK` |
| `GET` | `/api/stats` | Live operations telemetry (events, registrations, capacity %, upcoming) | `200 OK` |
| `POST` | `/api/events` | *[Stretch Goal 1]* Add a new fest event | `201 Created` / `400` |
| `DELETE` | `/api/registrations/{id}` | *[Stretch Goal 2]* Cancel registration and release seat | `200 OK` / `404` |

---

## 🧪 Testing with Postman

1. Open **Postman**.
2. Click **Import** in the top left.
3. Select `zenith26-fest-system/zenith26-api.postman_collection.json`.
4. Ensure the FastAPI backend is running (`uvicorn backend.main:app --reload`).
5. Run the requests sequentially:
   - `1. GET /api/events (All Events)`
   - `1b. GET /api/events (Filter by Category & Search)`
   - `2. GET /api/events/{event_id} (Success)`
   - `2b. GET /api/events/{event_id} (404 Not Found)`
   - `3a. POST /api/events/{event_id}/register (Success - Valid Body)`
   - `3b. POST /api/events/{event_id}/register (400 - Missing Required Field)`
   - `3c. POST /api/events/{event_id}/register (400 - Invalid Email without @)`
   - `3d. POST /api/events/{event_id}/register (409 - Duplicate Email)`
   - `4. GET /api/registrations`
   - `5. GET /api/stats`
   - `6. POST /api/events (Admin Create Event)`
   - `7. DELETE /api/registrations/{id} (Admin Cancel Registration)`
6. Refer to `Postman-Testing-Commands-Zenith26.md` for full command and payload details.
