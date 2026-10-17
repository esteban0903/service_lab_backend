# Task Assignment App

A technical assessment application for task scheduling and worker assignment based on daily capacity, skill matching, and business rules.

## Tech Stack (Phase 0)
- **Java**: 21
- **Framework**: Spring Boot 3.5.16
- **Build Tool**: Maven
- **Database**: H2 (in-memory, default `dev` profile) / PostgreSQL (`prod` profile)
- **Database Migrations**: Flyway

## Requirements
- Java 21+
- Maven 3.9+

## Getting Started

### Build
To compile and package the application:
- **Linux/macOS**:
  ```bash
  ./scripts/build.sh
  ```
- **Windows**:
  ```cmd
  scripts\build.bat
  ```
*(or run `mvn clean package -DskipTests` inside `/backend`)*

### Run
To start the backend with the default `dev` profile (in-memory H2 database):
- **Linux/macOS**:
  ```bash
  ./scripts/run.sh
  ```
- **Windows**:
  ```cmd
  scripts\run.bat
  ```
*(or run `mvn spring-boot:run` inside `/backend`)*

The server will start on port `8080`. The H2 console is accessible at `http://localhost:8080/h2-console` with JDBC URL `jdbc:h2:mem:taskdb` and user `sa` (empty password).

### Test
To run the automated tests:
- **Linux/macOS**:
  ```bash
  ./scripts/test.sh
  ```
- **Windows**:
  ```cmd
  scripts\test.bat
  ```
*(or run `mvn clean test` inside `/backend`)*

## Database Configuration
- **Dev Profile (default)**: Uses H2 in-memory with PostgreSQL compatibility mode. No setup required.
- **Prod Profile**: Configured via environment variables:
  - `DB_URL`: JDBC PostgreSQL connection string (e.g., `jdbc:postgresql://host:5432/dbname`)
  - `DB_USER`: Database username
  - `DB_PASS`: Database password

## Demo Data and Showcase Scripts

### Seeding Demo Data
To populate the database with demo workers, tasks, and initial assignments:
- **Linux/macOS**:
  ```bash
  ./scripts/seed.sh
  ```
- **Windows**:
  ```cmd
  scripts\seed.bat
  ```

> **Note**: Seeding is enabled by default in the `dev` profile (`app.seed.enabled=true`). In `prod`, the endpoint returns 404 unless `SEED_ENABLED=true` is set. The seed operation is idempotent: it wipes existing data and re-inserts the sample dataset with dates calculated relative to next Monday.

### Running the End-to-End Demo Script
To see an end-to-end interactive showcase demonstrating business rule enforcement and the auto-assignment algorithm:
1. Start the backend:
   ```cmd
   scripts\run.bat
   ```
2. In a separate terminal, execute:
   - **Linux/macOS**:
     ```bash
     ./scripts/demo.sh
     ```
   - **Windows**:
     ```cmd
     scripts\demo.bat
     ```

### What the Demo Demonstrates
1. **Idempotent Seeding (`POST /api/seed`)**:
   - Creates 5 workers with varying capacities (4h, 6h, 8h) and skill sets (Java, Spring, SQL, React, Python, Docker, DevOps).
   - Creates 12 tasks representing diverse project demands (short tasks, tasks spanning weekends, tasks with no skills, tasks with missing skills).
   - Establishes 2 manual assignments, including one multi-day task crossing a weekend to prove weekend pauses.
2. **Skill Validation Rule Enforcement (`POST /api/assignments`)**:
   - Rejects an invalid manual assignment attempt (assigning a React task to Diego, who lacks React and Javascript skills) with `422 Unprocessable Entity`, detailing the missing skills.
3. **Valid Manual Assignment (`POST /api/assignments`)**:
   - Assigns the task to Bruno (who possesses the required skills), returning `201 Created` with computed start and end dates.
4. **Greedy Auto-Assignment Algorithm (`POST /api/assignments/auto`)**:
   - Automatically distributes remaining eligible tasks across workers based on the most constrained task first, earliest completion date, and load balancing tie-breakers.
   - Reports unassigned tasks with descriptive reasons:
     - `No worker has skills: [kotlin]` (skill nobody has).
     - `No single worker has all required skills: [python, react]` (skills exist across the team, but no individual worker has all of them).


