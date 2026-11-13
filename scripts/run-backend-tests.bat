@echo off
setlocal enabledelayedexpansion

REM ============================================================================
REM BorrowHub - Run Backend Automated Test Suite
REM ============================================================================

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "PROJECT_ROOT=%%~fI"

if not exist "%PROJECT_ROOT%\.env" (
    echo [BorrowHub ERROR] Missing .env. Copy .env.example to .env and configure local values.
    exit /b 1
)
for /f "usebackq eol=# tokens=1,* delims==" %%A in ("%PROJECT_ROOT%\.env") do if not "%%A"=="" set "%%A=%%B"

if not defined DB_PASSWORD (
    echo [BorrowHub ERROR] DB_PASSWORD is missing from .env.
    exit /b 1
)
if "!DB_PASSWORD:~0,13!"=="replace-with-" (
    echo [BorrowHub ERROR] DB_PASSWORD in .env still contains placeholder value. Please set a real password.
    exit /b 1
)

if not defined JWT_SECRET (
    echo [BorrowHub ERROR] JWT_SECRET is missing from .env.
    exit /b 1
)
if "!JWT_SECRET:~0,8!"=="generate" (
    echo [BorrowHub ERROR] JWT_SECRET in .env still contains placeholder value. Please generate a unique secret.
    exit /b 1
)

if not defined BORROWHUB_SEED_ADMIN_PASSWORD (
    echo [BorrowHub ERROR] BORROWHUB_SEED_ADMIN_PASSWORD is missing from .env.
    exit /b 1
)
if not defined BORROWHUB_SEED_FACULTY_PASSWORD (
    echo [BorrowHub ERROR] BORROWHUB_SEED_FACULTY_PASSWORD is missing from .env.
    exit /b 1
)
if not defined BORROWHUB_SEED_STAFF_PASSWORD (
    echo [BorrowHub ERROR] BORROWHUB_SEED_STAFF_PASSWORD is missing from .env.
    exit /b 1
)
if not defined BORROWHUB_SEED_STUDENT_PASSWORD (
    echo [BorrowHub ERROR] BORROWHUB_SEED_STUDENT_PASSWORD is missing from .env.
    exit /b 1
)

echo ============================================================================
echo [BorrowHub] Executing Spring Boot Backend Test Suites (JUnit 5 / AssertJ)
echo ============================================================================

cd /d "%PROJECT_ROOT%\borrowhub-backend"

REM Ensure MySQL database is accessible for integration tests
netstat -ano | findstr /R /C:":3306 .*LISTENING" >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [BorrowHub] Starting MySQL database for test execution...
    call "%SCRIPT_DIR%start-database.bat"
)

call mvn test
set "TEST_EXIT_CODE=%ERRORLEVEL%"

cd /d "%PROJECT_ROOT%"

echo ============================================================================
if %TEST_EXIT_CODE% EQU 0 (
    echo [BorrowHub RESULT] BACKEND TESTS: PASSED - ALL ARCHITECTURE TESTS GREEN
) else (
    echo [BorrowHub RESULT] BACKEND TESTS: FAILED - EXIT CODE: %TEST_EXIT_CODE%
)
echo ============================================================================
exit /b %TEST_EXIT_CODE%
