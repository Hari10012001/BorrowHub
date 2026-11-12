@echo off
setlocal enabledelayedexpansion

REM ============================================================================
REM BorrowHub - Comprehensive System Health Check
REM ============================================================================

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "PROJECT_ROOT=%%~fI"

echo ============================================================================
echo [BorrowHub] Running Comprehensive System Health Check
echo ============================================================================

set "OVERALL_STATUS=0"

REM 1. Basic Dependencies
echo [1/5] Checking Toolchains and Dependencies...
where java >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    for /f "tokens=*" %%V in ('java -version 2^>^&1 ^| findstr /I "version"') do echo   [PASS] Java: %%V
) else (
    echo   [FAIL] Java JDK is not installed or not in PATH.
    set "OVERALL_STATUS=1"
)

where mvn >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    for /f "tokens=*" %%V in ('mvn -v 2^>^&1 ^| findstr /I "Apache Maven"') do echo   [PASS] Maven: %%V
) else (
    echo   [FAIL] Maven is not installed or not in PATH.
    set "OVERALL_STATUS=1"
)

where node >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    for /f "tokens=*" %%V in ('node -v 2^>^&1') do echo   [PASS] Node.js: %%V
) else (
    echo   [FAIL] Node.js is not installed or not in PATH.
    set "OVERALL_STATUS=1"
)

REM 2. MySQL Database Health
echo.
echo [2/5] Checking MySQL Database Service on Port 3306...
netstat -ano | findstr /R /C:":3306 .*LISTENING" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo   [PASS] MySQL port 3306 is LISTENING.
) else (
    echo   [FAIL] MySQL is NOT listening on port 3306.
    set "OVERALL_STATUS=1"
)

REM 3. Backend Service Health
echo.
echo [3/5] Checking Backend Spring Boot Service on Port 8080...
netstat -ano | findstr /R /C:":8080 .*LISTENING" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo   [PASS] Backend port 8080 is LISTENING.
    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
        "try { $h = Invoke-RestMethod -Uri 'http://localhost:8080/actuator/health' -TimeoutSec 3; if ($h.status -eq 'UP') { Write-Host '  [PASS] Backend Actuator Health: UP'; exit 0 } else { Write-Host ('  [FAIL] Actuator Status: ' + $h.status); exit 1 } } catch { Write-Host '  [FAIL] Actuator endpoint failed to respond'; exit 1 }"
    if !ERRORLEVEL! NEQ 0 set "OVERALL_STATUS=1"
) else (
    echo   [FAIL] Backend is NOT listening on port 8080.
    set "OVERALL_STATUS=1"
)

REM 4. Backend REST API Functional Response
echo.
echo [4/5] Checking Public REST API on /api/resources...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "try { $res = Invoke-RestMethod -Uri 'http://localhost:8080/api/resources' -TimeoutSec 3; Write-Host ('  [PASS] Public Resource API reachable [Catalog items count: ' + $res.Count + ']'); exit 0 } catch { Write-Host '  [FAIL] /api/resources failed to respond'; exit 1 }"
if %ERRORLEVEL% NEQ 0 set "OVERALL_STATUS=1"

REM 5. Frontend Web Server Health
echo.
echo [5/5] Checking Frontend Web Server on Port 5173...
netstat -ano | findstr /R /C:":5173 .*LISTENING" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo   [PASS] Frontend port 5173 is LISTENING.
    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
        "try { $f = Invoke-WebRequest -Uri 'http://localhost:5173' -UseBasicParsing -TimeoutSec 3; Write-Host ('  [PASS] Frontend HTTP response: ' + $f.StatusCode); exit 0 } catch { Write-Host '  [FAIL] Frontend web server unreachable'; exit 1 }"
    if !ERRORLEVEL! NEQ 0 set "OVERALL_STATUS=1"
) else (
    echo   [FAIL] Frontend is NOT listening on port 5173.
    set "OVERALL_STATUS=1"
)

echo.
echo ============================================================================
if %OVERALL_STATUS% EQU 0 (
    echo [BorrowHub RESULT] OVERALL SYSTEM HEALTH: HEALTHY - ALL CHECKS PASSED
) else (
    echo [BorrowHub RESULT] OVERALL SYSTEM HEALTH: UNHEALTHY - FAILURES DETECTED
)
echo ============================================================================
exit /b %OVERALL_STATUS%
