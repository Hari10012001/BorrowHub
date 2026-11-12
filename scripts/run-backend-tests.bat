@echo off
setlocal enabledelayedexpansion

REM ============================================================================
REM BorrowHub - Run Backend Automated Test Suite
REM ============================================================================

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "PROJECT_ROOT=%%~fI"

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
    echo [BorrowHub RESULT] BACKEND TESTS: PASSED (ALL ARCHITECTURE TESTS GREEN)
) else (
    echo [BorrowHub RESULT] BACKEND TESTS: FAILED (EXIT CODE: %TEST_EXIT_CODE%)
)
echo ============================================================================
exit /b %TEST_EXIT_CODE%
