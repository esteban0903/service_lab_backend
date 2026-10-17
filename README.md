# ServiceLab — Task Assignment & Scheduler

A production-ready technical assessment application for capacity-constrained task scheduling, worker skill matching, and business-rule-driven project allocations.

---

## 1. Project Overview & Requirements Coverage

The application schedules tasks onto workers while respecting working calendars, daily capacities, existing allocations, and strict skill matching.

| Requirement | Description | Implementing Backend Endpoints | Implementing Frontend Views |
| :--- | :--- | :--- | :--- |
| **Req 1: CRUD & Capacity** | Manage tasks (name, effort hours, status, skills) and workers (name, daily capacity 1–24h, skills). | `GET/POST /api/tasks`<br>`GET/PUT/DELETE /api/tasks/{id}`<br>`GET/POST /api/workers`<br>`GET/PUT/DELETE /api/workers/{id}` | **Tasks View** (`/tasks`)<br>**Workers View** (`/workers`) |
| **Req 2: Calendar & Manual Assignment** | Pure working-day calendar (Mon–Fri). Non-working days and weekend start dates shift to Monday. Partial day allocations respect remaining daily capacity. | `POST /api/assignments`<br>`GET /api/assignments`<br>`DELETE /api/assignments/{id}` | **Assignments View** (`/assignments`)<br>**Workload View** (`/workload`) |
| **Req 3: Business Invariants** | Strict skill matching ($S_{task} \subseteq S_{worker}$), one assignment per task, worker deletion/capacity checks, immutable DONE tasks. | Enforced in `AssignmentService`, `TaskService`, `WorkerService` with standard HTTP error codes (`400`, `404`, `409`, `422`). | All modal forms and action banners in UI |
| **Req 4: Deterministic Auto-Assignment** | Greedy scheduling heuristic: most constrained tasks first, earliest completion date, load balancing tie-breakers. Simulates without database queries in loop. | `POST /api/assignments/auto` | **Assignments View** (Auto-Assign modal & report section) |

---

## 2. Architecture & Project Structure

The project is structured as a decoupled client-server architecture:
- **Backend**: Java 21, Spring Boot 3.5, Spring Data JPA, Flyway migrations, and pure domain scheduling logic (`CalendarService`).
- **Frontend**: React 19, TypeScript, Vite, and plain modern CSS (no third-party UI component libraries or global state managers).

```
ServiceLab/
├── backend/
│   ├── src/main/java/com/example/taskassignment/
│   │   ├── config/              # AppConfig, CorsConfig, DatabaseUrlValidatorInitializer
│   │   ├── controller/          # TaskController, WorkerController, AssignmentController, ConfigController, HealthController
│   │   ├── dto/                 # Immutable Request/Response records & validation annotations
│   │   ├── exception/           # GlobalExceptionHandler & domain exceptions (NotFound, Conflict, Unprocessable)
│   │   ├── model/               # JPA entities: Task, Worker, Assignment, AssignmentDay
│   │   ├── repository/          # Spring Data JPA repositories with custom JPQL queries
│   │   ├── service/             # Pure CalendarService, AssignmentService, AutoAssignmentService, SeedService
│   │   └── util/                # Skill normalization utilities
│   ├── src/main/resources/
│   │   ├── db/migration/        # V1__init_schema.sql (PostgreSQL/H2 Flyway migration)
│   │   ├── application.yml      # Base Spring Boot configuration
│   │   ├── application-dev.yml  # H2 in-memory profile
│   │   └── application-prod.yml # PostgreSQL production profile
│   ├── Dockerfile               # Multi-stage Docker build (Maven + Temurin JRE 21)
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── api/                 # Typed API client with automatic cold-start retry
│   │   ├── components/          # Reusable Modal, AlertBanner, ChipList
│   │   ├── types/               # TypeScript interfaces matching backend DTOs
│   │   ├── utils/               # Pure date formatters (UTC-safe), workload grid calculations, retry backoff
│   │   ├── views/               # TasksView, WorkersView, AssignmentsView, WorkloadView
│   │   ├── App.tsx              # Top-level navigation, seed trigger, waking banner
│   │   └── index.css            # Vanilla CSS styling with design tokens & responsive grid
│   ├── package.json
│   └── vite.config.ts
├── scripts/                     # Cross-platform CLI scripts (.sh and .bat)
├── docker-compose.yml           # Local production stack (PostgreSQL 16 + Backend)
├── .env.example                 # Reference environment variables
└── README.md
```

---

## 3. Prerequisites

- **Java**: 21 LTS (`java -version`)
- **Maven**: 3.9+ (`mvn -version`)
- **Node.js**: 20+ LTS (`node -v`)
- **npm**: 10+ (`npm -v`)
- *(Optional)* **Docker & Docker Compose**: For running containerized PostgreSQL and backend.

---

## 4. How to Build, Run, and Test

### 1-Command CLI Execution

| Action | Linux / macOS (`.sh`) | Windows (`.bat`) | Direct Tool Command |
| :--- | :--- | :--- | :--- |
| **Build Backend** | `./scripts/build.sh` | `scripts\build.bat` | `cd backend && mvn clean package -DskipTests` |
| **Run Backend (`dev`)** | `./scripts/run.sh` | `scripts\run.bat` | `cd backend && mvn spring-boot:run` |
| **Test Backend** | `./scripts/test.sh` | `scripts\test.bat` | `cd backend && mvn test` |
| **Build Frontend** | `./scripts/build-frontend.sh` | `scripts\build-frontend.bat` | `cd frontend && npm run build` |
| **Run Frontend (`dev`)** | `./scripts/run-frontend.sh` | `scripts\run-frontend.bat` | `cd frontend && npm run dev` |
| **Test Frontend** | `./scripts/test-frontend.sh` | `scripts\test-frontend.bat` | `cd frontend && npm test` |

### Running the App Locally (Full Stack)
1. **Start Backend**: Run `./scripts/run.sh` (or `scripts\run.bat`). The server starts on `http://localhost:8080`.
   - H2 Console: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:taskdb`, User: `sa`, Password: *(empty)*).
   - Health check: `http://localhost:8080/api/health`.
2. **Start Frontend**: In another terminal, run `./scripts/run-frontend.sh` (or `scripts\run-frontend.bat`).
3. Open `http://localhost:5173` in your browser. The Vite development proxy forwards all `/api/*` calls to port `8080`.

### Testing the Production Profile Locally
You can test the application against a real PostgreSQL instance (e.g., a free database on [Neon](https://neon.tech) or local PostgreSQL) using the `prod` profile:

1. **Set Environment Variables**:
   - **Linux / macOS**:
     ```bash
     export SPRING_PROFILES_ACTIVE=prod
     export DB_URL="jdbc:postgresql://ep-xyz.us-east-2.aws.neon.tech/neondb?sslmode=require"
     export DB_USER="neondb_owner"
     export DB_PASS="your_neon_password"
     export SEED_ENABLED=true
     ```
   - **Windows (PowerShell)**:
     ```powershell
     $env:SPRING_PROFILES_ACTIVE="prod"
     $env:DB_URL="jdbc:postgresql://ep-xyz.us-east-2.aws.neon.tech/neondb?sslmode=require"
     $env:DB_USER="neondb_owner"
     $env:DB_PASS="your_neon_password"
     $env:SEED_ENABLED="true"
     ```
   - **Windows (CMD)**:
     ```cmd
     set SPRING_PROFILES_ACTIVE=prod
     set DB_URL=jdbc:postgresql://ep-xyz.us-east-2.aws.neon.tech/neondb?sslmode=require
     set DB_USER=neondb_owner
     set DB_PASS=your_neon_password
     set SEED_ENABLED=true
     ```
2. **Start the Backend**:
   ```bash
   cd backend && mvn spring-boot:run
   ```
   *Flyway will automatically execute `V1__init_schema.sql` against the real PostgreSQL database and validate the schema.*
3. **Run Seed & Demo in a Separate Terminal**:
   ```bash
   # Seed demo dataset
   ./scripts/seed.sh       # or scripts\seed.bat
   
   # Run full end-to-end showcase
   ./scripts/demo.sh       # or scripts\demo.bat
   ```

---

## 5. Demo Data & Showcase Scripts

### Seeding Demo Data (`POST /api/seed`)
Populate the database with a curated sample dataset (5 workers, 12 tasks, 2 manual assignments):
- **Linux/macOS**: `./scripts/seed.sh`
- **Windows**: `scripts\seed.bat`
- **UI**: Click the **"🌱 Reset & Seed Data"** button in the top navigation bar.

> **Idempotency Note**: The seed endpoint deletes existing assignments, tasks, and workers, and creates fresh demo data. The seed dates are calculated dynamically relative to the next working Monday of the configured application `Clock`, keeping demo dates current.

### Automated End-to-End Showcase (`demo.sh` / `demo.bat`)
Run an interactive demonstration of rule validation and auto-assignment:
- **Linux/macOS**: `./scripts/demo.sh`
- **Windows**: `scripts\demo.bat`

**What the demo exercises:**
1. **Idempotent Seed**: Populates workers (4h, 6h, 8h capacities) and tasks.
2. **Skill Rule Rejection**: Attempts to assign a React task to Diego (who lacks React) $\rightarrow$ receives `422 Unprocessable Entity` listing missing skills.
3. **Valid Assignment**: Assigns the task to Bruno $\rightarrow$ receives `201 Created` with computed start/end dates.
4. **Auto-Assignment Execution**: Runs greedy auto-assignment $\rightarrow$ balances load across workers and outputs clear diagnostics for unassigned tasks (e.g., `No worker has skills: [kotlin]`).

---

## 6. Calendar Assumptions & Business Rules

### Working Calendar Logic (`CalendarService`)
- **Work Days**: Monday through Friday only. Saturdays and Sundays are non-working days.
- **Weekend Shift**: If a requested `startDate` falls on Saturday or Sunday, the effective start date shifts forward to the next Monday.
- **Daily Capacity & Free Hours**: A worker has a fixed `hoursPerDay`. For any date, $\text{free capacity} = \max(0, \text{hoursPerDay} - \text{existing load})$.
- **Skipping Days**: If a date has 0 free capacity (or is overbooked), that day is skipped.
- **Sequential Placement**: The task consumes $\min(\text{remainingEffort}, \text{freeCapacity})$ on each available working day until 0 hours remain.
- **End Date**: The `endDate` is the last date on which at least 1 hour of the task was allocated.

### Business Invariants & HTTP Status Codes
- **Skill Invariant**: A worker can only be assigned to a task if the worker's skills are a superset of the task's required skills ($S_{task} \subseteq S_{worker}$). If any skill is missing, the API rejects with `422 Unprocessable Entity` and lists the missing skills. Tasks with no required skills can be assigned to any worker.
- **One Worker per Task**: A task has at most one assignment (`409 Conflict` if already assigned).
- **Completed Tasks**: Tasks marked `DONE` cannot be assigned (`409 Conflict`), nor can their effort or required skills be modified (`409 Conflict`).
- **Worker Deletion / Capacity Modification**:
  - A worker with active assignments cannot be deleted (`409 Conflict`).
  - A worker's `hoursPerDay` cannot be reduced below the peak daily hours already allocated to them in any existing assignment day (`409 Conflict`).
- **Cascade Cleanup**: Deleting an assigned task removes its assignment and daily breakdown records (`AssignmentDay`).

---

## 7. Auto-Assignment Algorithm

### Algorithm Steps (`AutoAssignmentService`)
The auto-assignment algorithm is **deterministic** and **greedy**:

1. **Candidate Retrieval**:
   - Fetches all tasks where `status != DONE` and no assignment exists.
   - Loads all workers and their current load map ($\text{date} \to \text{allocated hours}$) into memory.
2. **Task Sorting (Most Constrained First)**:
   - Evaluates how many workers have the required skills for each task: $K = |\{w \in \text{Workers} \mid S_{task} \subseteq S_w\}|$.
   - Sorts candidate tasks ascending by $K$. If tied, sorts descending by `effortHours`. If tied, sorts ascending by `taskId`.
3. **Worker Evaluation & Placement**:
   - For each task, finds all qualifying workers ($S_{task} \subseteq S_w$).
   - If no worker qualifies, the task is marked unassigned with an explicit explanation:
     - If no individual worker in the system has skill $X$: `"No worker has skills: [X]"`
     - If all individual skills exist in the team but no single worker possesses the full combination: `"No single worker has all required skills: [X, Y]"`
   - For each qualifying worker, simulates placement using `CalendarService.plan(...)` starting from `startDate` against the in-memory load map.
   - Selects the best worker based on:
     1. **Earliest `endDate`** (finishes earliest).
     2. **Least total hours already assigned** (load balancing across the entire schedule).
     3. **Smallest `workerId`** (deterministic tie-breaker).
4. **Schedule Update & Persistence**:
   - Updates the chosen worker's in-memory load map and total assigned hours so subsequent tasks see the newly placed load.
   - Persists all assignments within a single transaction at the end of the simulation.

### Small Worked Example
Suppose two workers are available starting Monday:
- **Alice** (8h/day, Java), current total load: 0h.
- **Bob** (8h/day, Java), current total load: 4h on Monday.

Task $T_1$ (8h, Java) needs scheduling:
- Simulating with Alice: Monday has 8h free $\rightarrow$ finishes **Monday**.
- Simulating with Bob: Monday has 4h free, Tuesday has 8h free $\rightarrow$ finishes **Tuesday**.
- Alice finishes earlier (Monday vs. Tuesday) $\rightarrow$ Assigned to **Alice**. Alice's Monday load is now 8h.

Task $T_2$ (4h, Java) comes next:
- Simulating with Alice: Monday has 0h free $\rightarrow$ scheduled Tuesday (4h) $\rightarrow$ finishes **Tuesday**.
- Simulating with Bob: Monday has 4h free $\rightarrow$ scheduled Monday (4h) $\rightarrow$ finishes **Monday**.
- Bob finishes earlier (Monday vs. Tuesday) $\rightarrow$ Assigned to **Bob**.

### Limitations of the Algorithm
1. **Greedy Heuristic**: Decisions made for earlier tasks are not re-evaluated for global optimality (it does not solve NP-hard multi-processor job-shop scheduling via integer linear programming).
2. **No Dynamic Reassignment**: Existing assignments are strictly respected and never rescheduled or shifted.
3. **No Priorities or Deadlines**: Tasks do not support explicit priority ranks or due dates; ordering is driven strictly by skill scarcity and effort.
4. **Single-Phase Concurrency**: Concurrency control is enforced transactionally; concurrent auto-assign requests run in isolated database transactions.

---

## 8. API Reference

All error responses follow the standard JSON payload:
```json
{
  "timestamp": "2026-10-17T18:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "fieldErrors": { "effortHours": "Effort hours must be greater than zero" }
}
```

| Method | Endpoint | Success Code | Description | Error Codes |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/health` | `200 OK` | Lightweight service health probe | — |
| `GET` | `/api/config` | `200 OK` | Returns application configuration (`seedEnabled`) | — |
| `GET` | `/api/tasks` | `200 OK` | List all tasks | — |
| `POST` | `/api/tasks` | `201 Created` | Create new task (validates effort > 0, normalizes skills) | `400` |
| `GET` | `/api/tasks/{id}` | `200 OK` | Get task by ID | `404` |
| `PUT` | `/api/tasks/{id}` | `200 OK` | Update task (blocked if task is `DONE`) | `400`, `404`, `409` |
| `DELETE` | `/api/tasks/{id}` | `204 No Content` | Delete task (cascades to assignment) | `404` |
| `GET` | `/api/workers` | `200 OK` | List all workers | — |
| `POST` | `/api/workers` | `201 Created` | Create worker (validates hours 1–24) | `400` |
| `GET` | `/api/workers/{id}` | `200 OK` | Get worker by ID | `404` |
| `PUT` | `/api/workers/{id}` | `200 OK` | Update worker (capacity cannot drop below max allocated day) | `400`, `404`, `409` |
| `DELETE` | `/api/workers/{id}` | `204 No Content` | Delete worker (blocked if assigned to tasks) | `404`, `409` |
| `GET` | `/api/assignments` | `200 OK` | List all active assignments with daily breakdowns | — |
| `POST` | `/api/assignments` | `201 Created` | Manually assign worker to task starting on date | `400`, `404`, `409`, `422` |
| `DELETE` | `/api/assignments/{id}` | `204 No Content` | Remove assignment (unassign task) | `404` |
| `POST` | `/api/assignments/auto` | `200 OK` | Run greedy auto-assignment simulation | `200` |
| `POST` | `/api/seed` | `200 OK` | Reset DB and load demo data (requires seed enabled) | `404` (if disabled) |

---

## 9. Cloud Deployment Guide (Neon + Render + Vercel)

Follow these steps in order to deploy the application to cloud services.

### Step 1: Managed Database (Neon PostgreSQL)
1. Create a project at [Neon](https://neon.tech) and create a database named `taskdb`.
2. Retrieve the connection string. Neon provides connection strings in URI format:
   `postgresql://neondb_owner:YOUR_PASSWORD@ep-xyz.us-east-2.aws.neon.tech/neondb?sslmode=require`
3. **Format as JDBC URL**: Prepend `jdbc:` to the connection string:
   `jdbc:postgresql://neondb_owner:YOUR_PASSWORD@ep-xyz.us-east-2.aws.neon.tech/neondb?sslmode=require`

### Step 2: Backend Web Service (Render)
1. Create a new **Web Service** on [Render](https://render.com) connected to your repository.
2. Select **Docker** environment (or set Root Directory to `backend` with Dockerfile).
3. Set the **Health Check Path** to `/api/health`.
4. Configure the following **Environment Variables**:
   - `SPRING_PROFILES_ACTIVE`: `prod`
   - `DB_URL`: `jdbc:postgresql://<host>/<db>?sslmode=require` *(must start with `jdbc:`)*
   - `DB_USER`: `<username>`
   - `DB_PASS`: `<password>`
   - `SEED_ENABLED`: `true` *(or `false` for locked production)*
   - `CORS_ALLOWED_ORIGINS`: `https://your-frontend.vercel.app` *(update once Vercel URL is known)*
   - `APP_TIMEZONE`: `America/Bogota`
5. Deploy the backend service. Flyway will automatically run `V1__init_schema.sql` on startup.

### Step 3: Frontend Single-Page App (Vercel)
1. Import the repository on [Vercel](https://vercel.com).
2. Set **Root Directory** to `frontend`.
3. Build & Development Settings:
   - Framework Preset: **Vite**
   - Build Command: `npm run build`
   - Output Directory: `dist`
4. Configure Environment Variables:
   - `VITE_API_URL`: `https://your-backend.onrender.com` *(your Render backend URL)*
5. Deploy.
6. Copy the assigned Vercel URL (e.g., `https://servicelab.vercel.app`) and add it to `CORS_ALLOWED_ORIGINS` in Render.

### Cold-Start Handling & Troubleshooting
- **Render Free Tier Spin-Down**: Render free instances enter sleep mode after 15 minutes of inactivity. When a request arrives, booting takes ~30–60 seconds.
- **Built-in Frontend Protection**: The frontend automatically detects requests exceeding 3 seconds or failing with 502/network errors. It displays an amber banner:
  > *⏳ The server is waking up, this can take up to 2 minutes...*
  and retries with exponential backoff for up to 150 seconds. Once the container is ready, data renders seamlessly without user intervention.
- **Troubleshooting Common Issues**:
  - *Startup Error: DB_URL must start with 'jdbc:'*: The backend includes a strict startup validator (`DatabaseUrlValidatorInitializer`). Ensure `DB_URL` begins with `jdbc:postgresql://`.
  - *CORS blocked*: Verify `CORS_ALLOWED_ORIGINS` in Render matches your Vercel URL (without a trailing slash).

---

## 10. Test Coverage & Verification

### What Was Tested
- **Backend Unit & Integration Tests (103 tests passing)**:
  - `CalendarServiceTest` (21 tests): Working day advance, weekend shifts, capacity saturation, month-boundary spans, load overbooking.
  - `TaskServiceTest` & `WorkerServiceTest` (23 tests): Entity CRUD, skill normalization, constraint checks.
  - `AssignmentServiceTest` (12 tests): Manual scheduling invariants, 422 missing skills, 409 already assigned/completed.
  - `AutoAssignmentServiceTest` (10 tests): Scarcity ordering, tie-breakers, deterministic load balancing, in-memory simulation.
  - `SeedServiceIntegrationTest` (4 tests): Idempotency, disabled 404, dynamic date calculations.
  - `HealthControllerApiTest` & `ConfigControllerApiTest` (2 tests): Health and config endpoints.
  - `DatabaseUrlValidatorInitializerTest` (5 tests): JDBC prefix and production environment validation.
  - `CorsIntegrationTest` & Controller API tests (26 tests): HTTP endpoints, validation errors, header assertions.
- **Frontend Vitest Tests (17 tests passing)**:
  - `date.test.ts` (4 tests): Timezone drift prevention (formatting `"2026-10-19"` as `"Mon 19 Oct"` in America/Bogota).
  - `workload.test.ts` (6 tests): Monday calculations, 5-day grid allocations, workload status thresholds.
  - `retry.test.ts` (7 tests): Exponential backoff, retryable vs non-retryable error identification, 150-second timeout enforcement.

### What Was Not Tested
- **Physical Browser Rendering**: Headless CLI environments do not verify GPU-rendered CSS hover tooltip animations or manual pointer clicks.
- **Live Cloud Network Latency**: Cloud latency variations and physical load balancing across geographically distant regions.

### Improvements with More Time
1. **Dynamic Rescheduling / Shifting**: Allow existing manual assignments to be shifted to accommodate higher-priority tasks.
2. **Priorities & Deadlines**: Introduce task priority levels and target completion dates into the auto-assignment objective function.
3. **Public Holidays Calendar**: Support country-specific bank holiday lists alongside weekends.
4. **Global Optimization Solver**: Implement an exact solver (e.g. OptaPlanner / Timefold or integer linear programming) for globally optimal load distribution.

---

## 11. Demo Credentials & Transparency Note

- **Demo Credentials**: *Not applicable* — Authentication is not included in this release per SPEC requirements.
- **Transparency Note**: AI-assisted development tools (Claude 3.7 / Google Gemini) were utilized for code structuring and pair programming. All architecture, domain algorithms, test suites, and business invariants were thoroughly reviewed, validated, and verified.
