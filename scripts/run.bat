@echo off
setlocal
cd /d "%~dp0\..\backend"

echo Starting backend in dev mode (H2 in-memory)...
call mvn spring-boot:run -Dspring-boot.run.profiles=dev

