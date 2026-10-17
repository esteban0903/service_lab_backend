@echo off
setlocal
cd /d "%~dp0\..\frontend"

echo Running frontend test suite...
call npm test
if %ERRORLEVEL% neq 0 (
    echo Frontend tests failed with error code %ERRORLEVEL%.
    exit /b %ERRORLEVEL%
)
echo All frontend tests passed.
