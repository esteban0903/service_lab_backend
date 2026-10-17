@echo off
setlocal

set BASE_URL=%BASE_URL%
if "%BASE_URL%"=="" set BASE_URL=http://localhost:8080

echo ==========================================
echo Seeding demo data via POST %BASE_URL%/api/seed
echo ==========================================
curl.exe -s -X POST "%BASE_URL%/api/seed" -H "Content-Type: application/json"
echo.
endlocal

