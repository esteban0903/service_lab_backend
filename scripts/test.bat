@echo off
setlocal
cd /d "%~dp0\..\backend"

echo Running backend test suite...
call mvn clean test
if %ERRORLEVEL% neq 0 (
    echo Tests failed with error code %ERRORLEVEL%.
    exit /b %ERRORLEVEL%
)
echo All tests passed.

