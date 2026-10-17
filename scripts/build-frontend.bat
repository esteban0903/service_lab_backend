@echo off
setlocal
cd /d "%~dp0\..\frontend"

if not exist "node_modules" (
  echo Installing frontend dependencies...
  call npm install
)

echo Building frontend application...
call npm run build
if %ERRORLEVEL% neq 0 (
  echo Frontend build failed with error code %ERRORLEVEL%.
  exit /b %ERRORLEVEL%
)
echo Frontend build successful.

