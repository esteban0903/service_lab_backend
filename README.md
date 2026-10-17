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

