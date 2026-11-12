@echo off
setlocal enabledelayedexpansion

REM ============================================================================
REM BorrowHub - Run Frontend Automated Test Suite (Vitest & Build Check)
REM ============================================================================

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "PROJECT_ROOT=%%~fI"

echo ============================================================================
echo [BorrowHub] Executing Frontend Quality Assurance (Vitest / React Testing Library)
echo ============================================================================

cd /d "%PROJECT_ROOT%\borrowhub-frontend"

echo [1/2] Running Component and Unit Tests...
call npm test
set "TEST_EXIT_CODE=%ERRORLEVEL%"

if %TEST_EXIT_CODE% NEQ 0 (
    echo [BorrowHub ERROR] Frontend Vitest tests failed.
    cd /d "%PROJECT_ROOT%"
    exit /b %TEST_EXIT_CODE%
)

echo.
echo [2/2] Running Production Build Verification (Vite)...
call npm run build
set "BUILD_EXIT_CODE=%ERRORLEVEL%"

cd /d "%PROJECT_ROOT%"

echo ============================================================================
if %BUILD_EXIT_CODE% EQU 0 (
    echo [BorrowHub RESULT] FRONTEND TESTS AND BUILD: PASSED (100%% SUCCESS)
) else (
    echo [BorrowHub RESULT] FRONTEND BUILD: FAILED (EXIT CODE: %BUILD_EXIT_CODE%)
)
echo ============================================================================
exit /b %BUILD_EXIT_CODE%
