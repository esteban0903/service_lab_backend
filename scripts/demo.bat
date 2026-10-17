@echo off
setlocal enabledelayedexpansion

set BASE_URL=%BASE_URL%
if "%BASE_URL%"=="" set BASE_URL=http://localhost:8080

echo =================================================================
echo                SERVICE LAB - DEMO SHOWCASE SCRIPT                
echo =================================================================
echo Target Base URL: %BASE_URL%
echo.

echo -----------------------------------------------------------------
echo STEP 1: Seed Demo Data
echo -----------------------------------------------------------------
echo Request: POST %BASE_URL%/api/seed
curl.exe -s -X POST "%BASE_URL%/api/seed" -H "Content-Type: application/json" > "%TEMP%\seed_resp.json"
type "%TEMP%\seed_resp.json"
echo.
echo.

for /f "delims=" %%i in ('powershell -NoProfile -Command "(Get-Content '%TEMP%\seed_resp.json' | ConvertFrom-Json).demoStartDate"') do set DEMO_START_DATE=%%i
if "%DEMO_START_DATE%"=="" set DEMO_START_DATE=2026-10-19

echo Resolved Demo Start Date from Seed: %DEMO_START_DATE%
echo.

rem Query tasks and workers to retrieve dynamic IDs by name
curl.exe -s "%BASE_URL%/api/tasks" > "%TEMP%\tasks_resp.json"
curl.exe -s "%BASE_URL%/api/workers" > "%TEMP%\workers_resp.json"

for /f "delims=" %%i in ('powershell -NoProfile -Command "$tasks = Get-Content '%TEMP%\tasks_resp.json' | ConvertFrom-Json; ($tasks | Where-Object { $_.name -eq 'Customer Portal Frontend' })[0].id"') do set TASK_ID=%%i
for /f "delims=" %%i in ('powershell -NoProfile -Command "$workers = Get-Content '%TEMP%\workers_resp.json' | ConvertFrom-Json; ($workers | Where-Object { $_.name -eq 'Diego' })[0].id"') do set DIEGO_ID=%%i
for /f "delims=" %%i in ('powershell -NoProfile -Command "$workers = Get-Content '%TEMP%\workers_resp.json' | ConvertFrom-Json; ($workers | Where-Object { $_.name -eq 'Bruno' })[0].id"') do set BRUNO_ID=%%i

echo Resolved IDs by Name:
echo   - Task 'Customer Portal Frontend': ID=%TASK_ID%
echo   - Worker 'Diego': ID=%DIEGO_ID%
echo   - Worker 'Bruno': ID=%BRUNO_ID%
echo.

echo -----------------------------------------------------------------
echo STEP 2: Attempt Invalid Manual Assignment (Missing Skills -^> 422)
echo -----------------------------------------------------------------
echo Attempting to assign 'Customer Portal Frontend' (requires [react, javascript])
echo to worker 'Diego' (skills: [python, sql, docker])...
echo Request: POST %BASE_URL%/api/assignments

powershell -NoProfile -Command "Set-Content -Path '%TEMP%\reject_payload.json' -Value ('{\"taskId\": ' + %TASK_ID% + ', \"workerId\": ' + %DIEGO_ID% + ', \"startDate\": \"%DEMO_START_DATE%\"}')"
type "%TEMP%\reject_payload.json"
echo.
echo.
echo HTTP Response:
curl.exe -s -i -X POST "%BASE_URL%/api/assignments" -H "Content-Type: application/json" -d "@%TEMP%\reject_payload.json"
echo.
echo.

echo -----------------------------------------------------------------
echo STEP 3: Perform Valid Manual Assignment (Matching Skills -^> 201)
echo -----------------------------------------------------------------
echo Assigning 'Customer Portal Frontend' to worker 'Bruno' (skills: [react, javascript, css])...
echo Request: POST %BASE_URL%/api/assignments

powershell -NoProfile -Command "Set-Content -Path '%TEMP%\valid_payload.json' -Value ('{\"taskId\": ' + %TASK_ID% + ', \"workerId\": ' + %BRUNO_ID% + ', \"startDate\": \"%DEMO_START_DATE%\"}')"
type "%TEMP%\valid_payload.json"
echo.
echo.
echo HTTP Response:
curl.exe -s -i -X POST "%BASE_URL%/api/assignments" -H "Content-Type: application/json" -d "@%TEMP%\valid_payload.json"
echo.
echo.

echo -----------------------------------------------------------------
echo STEP 4: Run Auto-Assignment Algorithm
echo -----------------------------------------------------------------
echo Request: POST %BASE_URL%/api/assignments/auto

powershell -NoProfile -Command "Set-Content -Path '%TEMP%\auto_payload.json' -Value '{\"startDate\": \"%DEMO_START_DATE%\"}'"
type "%TEMP%\auto_payload.json"
echo.
echo.
echo HTTP Response:
curl.exe -s -i -X POST "%BASE_URL%/api/assignments/auto" -H "Content-Type: application/json" -d "@%TEMP%\auto_payload.json"
echo.
echo.

echo =================================================================
echo                      DEMO SHOWCASE FINISHED                      
echo =================================================================

rem Cleanup temporary files
del "%TEMP%\seed_resp.json" 2>nul
del "%TEMP%\tasks_resp.json" 2>nul
del "%TEMP%\workers_resp.json" 2>nul
del "%TEMP%\reject_payload.json" 2>nul
del "%TEMP%\valid_payload.json" 2>nul
del "%TEMP%\auto_payload.json" 2>nul

endlocal
