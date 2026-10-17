# ServiceLab Codebase Tour

Welcome to the **ServiceLab** codebase. This guide walks you through the architecture, core domain abstractions, critical business rule implementations, and frontend structure.

---

## 1. Architectural Architecture & Design Principles

ServiceLab is structured into two decoupled tiers:
- **Backend**: Spring Boot 3.5 on Java 21, adhering to layered domain-driven design:
  - Controllers expose typed REST APIs and handle HTTP status codes.
  - Services implement business logic, state machines, and scheduling simulations.
  - Repositories encapsulate Spring Data JPA queries with custom JPQL aggregations.
  - Database schema lifecycle is strictly governed by Flyway (`V1__init_schema.sql`).
- **Frontend**: React 19 Single Page Application built with Vite and TypeScript:
  - Plain modern CSS using design tokens (no third-party UI libraries like MUI or Tailwind).
  - Pure calculation modules separated from UI components (`workload.ts`, `retry.ts`, `date.ts`).
  - Zero global state framework overhead; state is isolated to views with predictable top-level trigger events.

---

## 2. Backend Tour

### A. Pure Domain Scheduling: `CalendarService`
- **File**: `backend/src/main/java/com/example/taskassignment/service/CalendarService.java`
- **Purpose**: Pure Java scheduling calculator (no Spring annotations, no database, no system clock).
- **Key Method**: `plan(LocalDate startDate, int effortHours, int hoursPerDay, Map<LocalDate, Integer> existingLoad)`
- **Core Rules**:
  - Shifts weekend start dates (`SATURDAY`, `SUNDAY`) forward to the next Monday.
  - Iterates day-by-day (skipping weekends).
  - Computes $\text{free capacity} = \max(0, \text{hoursPerDay} - \text{existing load})$.
  - Allocates $\min(\text{remainingEffort}, \text{freeCapacity})$ per working day until 0 hours remain.
  - Returns an immutable `Placement` record with `effectiveStartDate`, `endDate`, and `List<DailyAllocation>`.

### B. Business Invariant Enforcement: `AssignmentService`
- **File**: `backend/src/main/java/com/example/taskassignment/service/AssignmentService.java`
- **Purpose**: Coordinates manual task assignments and guards business rules:
  - **Skill Subset Invariant**: Verifies $S_{task} \subseteq S_{worker}$. Throws `UnprocessableEntityException` (HTTP 422) detailing missing skills if incomplete.
  - **Task State Invariant**: Rejects completed (`DONE`) tasks or already assigned tasks with `ConflictException` (HTTP 409).
  - **Daily Breakdown Persistence**: Converts `Placement.allocations()` into `AssignmentDay` entities mapped to the database.

### C. Resource Lifecycle Invariants: `TaskService` & `WorkerService`
- **Files**:
  - `backend/src/main/java/com/example/taskassignment/service/TaskService.java`
  - `backend/src/main/java/com/example/taskassignment/service/WorkerService.java`
- **Key Invariants**:
  - Normalizes skills on input (trim, lowercase, deduplicate).
  - Block modifications to effort or required skills on tasks marked `DONE` (HTTP 409).
  - Blocks deletion of workers with existing assignments (HTTP 409).
  - Blocks reducing a worker's `hoursPerDay` below the maximum single-day allocation already scheduled for them (HTTP 409).

### D. Deterministic Auto-Assignment: `AutoAssignmentService`
- **File**: `backend/src/main/java/com/example/taskassignment/service/AutoAssignmentService.java`
- **Purpose**: Greedy multi-task scheduling algorithm:
  - Retrieves unassigned `NOT_DONE` tasks and pre-loads worker daily schedules into an in-memory load map.
  - Sorts candidate tasks by **most constrained first** (fewest qualifying workers $K$, then descending effort, then task ID).
  - Simulates assignment for all qualifying workers using `CalendarService.plan`.
  - Picks best worker based on:
    1. Earliest `endDate`.
    2. Lowest total historical allocated hours (load-balancing tie-breaker).
    3. Lowest `workerId` (deterministic tie-breaker).
  - Updates in-memory load maps so subsequent tasks observe the new allocation.
  - Persists all assignments within a single database transaction.

### E. Database Layer & Migrations
- **Migration**: `backend/src/main/resources/db/migration/V1__init_schema.sql`
  - Defines `tasks`, `task_skills`, `workers`, `worker_skills`, `assignments`, and `assignment_days`.
  - Strict foreign keys and unique constraints (`assignments.task_id UNIQUE`).
- **Fail-Fast Startup Validator**: `backend/src/main/java/com/example/taskassignment/config/DatabaseUrlValidatorInitializer.java`
  - Verifies presence and `jdbc:` prefix format for `DB_URL` under the `prod` profile before database initialization.

---

## 3. Frontend Tour

### A. Core Views (`frontend/src/views/`)
1. **AssignmentsView** (`AssignmentsView.tsx`):
   - Interactive calendar scheduling table.
   - Manual assignment modal (date picker, worker selection, skill mismatch warnings).
   - Auto-assignment trigger with simulation diagnostic report.
2. **WorkloadView** (`WorkloadView.tsx`):
   - 5-day working week matrix (Monday through Friday) showing per-worker capacity utilization (`used/capacity`).
   - Color coding for empty, partial, full, and overbooked days.
   - Week navigation toolbar with automatic default week selection (earliest assignment date).
   - Interactive breakdown modal when clicking any worker-day cell.
3. **TasksView** (`TasksView.tsx`):
   - Task list with status badges, effort hours, skill tag chips, and assigned worker indicators.
   - Modal form for creating and editing tasks with real-time validation.
4. **WorkersView** (`WorkersView.tsx`):
   - Worker directory with daily capacities (hours/day) and skill sets.
   - Modal form for creating and editing workers.

### B. Pure Utilities (`frontend/src/utils/`)
- **`workload.ts`**: Computes the Monday-to-Friday grid, determines week boundaries, and aggregates daily allocations from assignments.
- **`retry.ts`**: Wraps API calls with an exponential backoff retry loop (up to 150s) to handle server cold starts seamlessly, communicating waking state to the UI banner.
- **`date.ts`**: Parses and formats dates using UTC components (`Date.UTC`) to prevent timezone drift in negative UTC offset zones like `America/Bogota`.

### C. API Client (`frontend/src/api/client.ts`)
- Typed request wrapper that maps responses into strongly typed DTOs and unwraps standard API error payloads into structured `ApiError` instances.

---

## 4. Test Architecture

The repository enforces strict testing at all levels:
- **Unit Tests**:
  - `CalendarServiceTest`: Exhaustive testing of pure placement logic (21 tests).
  - `SkillUtilsTest`, `TaskServiceTest`, `WorkerServiceTest`, `AssignmentServiceTest`, `AutoAssignmentServiceTest`.
- **API & Integration Tests**:
  - `TaskControllerApiTest`, `WorkerControllerApiTest`, `AssignmentFlowIntegrationTest`, `AutoAssignmentIntegrationTest`, `CorsIntegrationTest`, `HealthControllerApiTest`, `ConfigControllerApiTest`.
- **Frontend Vitest Suite**:
  - `date.test.ts`, `workload.test.ts`, `retry.test.ts` (17 tests).
