@echo off
setlocal enabledelayedexpansion

REM ============================================================================
REM BorrowHub - Run Playwright End-to-End User Journey Tests
REM ============================================================================

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "PROJECT_ROOT=%%~fI"

echo ============================================================================
echo [BorrowHub] Executing Playwright End-to-End (E2E) Browser Test Suite
echo ============================================================================

REM Check if backend and frontend are running
netstat -ano | findstr /R /C:":8080 .*LISTENING" >nul 2>&1
set "BE_ACTIVE=%ERRORLEVEL%"

netstat -ano | findstr /R /C:":5173 .*LISTENING" >nul 2>&1
set "FE_ACTIVE=%ERRORLEVEL%"

if %BE_ACTIVE% NEQ 0 (
    echo [BorrowHub] Backend is not running. Starting application services...
    call "%SCRIPT_DIR%start-borrowhub.bat"
) else if %FE_ACTIVE% NEQ 0 (
    echo [BorrowHub] Frontend is not running. Starting application services...
    call "%SCRIPT_DIR%start-borrowhub.bat"
)

echo.
echo Running Playwright user journeys in headless Chromium...
cd /d "%PROJECT_ROOT%\borrowhub-frontend"
call npm run test:e2e
set "E2E_EXIT_CODE=%ERRORLEVEL%"

cd /d "%PROJECT_ROOT%"

echo ============================================================================
if %E2E_EXIT_CODE% EQU 0 (
    echo [BorrowHub RESULT] PLAYWRIGHT E2E TESTS: PASSED - ALL USER JOURNEYS GREEN
) else (
    echo [BorrowHub RESULT] PLAYWRIGHT E2E TESTS: FAILED - EXIT CODE: %E2E_EXIT_CODE%
)
echo ============================================================================
exit /b %E2E_EXIT_CODE%
