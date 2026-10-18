# ServiceLab — Task Assignment Backend

Spring Boot 3.5 application for capacity-constrained task scheduling, worker skill matching, and business-rule-driven project allocations.

---

## 1. Project Structure

```
.
├── src/
│   ├── main/java/com/example/taskassignment/
│   │   ├── config/              # Security and database configuration
│   │   ├── controller/          # REST controllers
│   │   ├── domain/              # JPA entities: Task, Worker, Assignment, AssignmentDay, User, Role
│   │   ├── dto/                 # Domain-divided records (task, worker, assignment, auth, skill, common)
│   │   ├── exception/           # Exception handlers
│   │   ├── repository/          # JPA repositories with custom queries
│   │   ├── security/            # JWT authentication and authorization
│   │   └── service/             # Domain logic: CalendarService, AssignmentService, AutoAssignmentService, SkillUtils
│   └── main/resources/
│       ├── db/migration/        # Flyway SQL migrations
│       ├── application.yml      # Base configuration
│       ├── application-dev.yml  # H2 profile
│       └── application-prod.yml # PostgreSQL profile
├── build.sh                     # Build script (mvn clean package)
├── Dockerfile                   # Multi-stage production container
├── pom.xml
└── README.md
```

---

## 2. Prerequisites

- **Java**: 21 LTS
- **Maven**: 3.9+
- *(Production)*: PostgreSQL 16+

---

## 3. How to Build, Run, and Test

### Compile Backend
```bash
./build.sh
# or: mvn clean package -DskipTests
```

### Run Locally (Dev Mode with H2)
```bash
mvn spring-boot:run
```
Starts on `http://localhost:8080`.
- Health check: `http://localhost:8080/api/health`
- H2 Console: `http://localhost:8080/h2-console` (`jdbc:h2:mem:taskdb`, user: `sa`, password: empty)

### Run Tests (162 tests)
```bash
mvn test
```

---

## 4. Demo Credentials & Authentication

- **Administrator**: `admin@demo.com` / `Admin123!` (Role: `ADMIN`)
- **Workers**:
  - `ana@demo.com` / `Worker123!` (Role: `WORKER`)
  - `bruno@demo.com` / `Worker123!` (Role: `WORKER`)
  - `carla@demo.com` / `Worker123!` (Role: `WORKER`)
  - `diego@demo.com` / `Worker123!` (Role: `WORKER`)
  - `elena@demo.com` / `Worker123!` (Role: `WORKER`)

---

## 5. Development

Developed using **Google Antigravity (Free Tier)**.
