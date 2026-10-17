@echo off
setlocal
cd /d "%~dp0\..\backend"

echo Building backend application...
call mvn clean package -DskipTests
if %ERRORLEVEL% neq 0 (
    echo Build failed with error code %ERRORLEVEL%.
    exit /b %ERRORLEVEL%
)
echo Build successful.

