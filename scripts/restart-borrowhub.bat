@echo off
setlocal enabledelayedexpansion

REM ============================================================================
REM BorrowHub - Master Application Restart Script
REM ============================================================================

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "PROJECT_ROOT=%%~fI"

echo ============================================================================
echo [BorrowHub] Initiating Clean Restart of BorrowHub Services...
echo ============================================================================

call "%SCRIPT_DIR%stop-borrowhub.bat"
if %ERRORLEVEL% NEQ 0 (
    echo [BorrowHub WARNING] Shutdown encountered issues, proceeding with caution...
)

echo.
echo Waiting 3 seconds before restart...
ping -n 4 127.0.0.1 >nul

call "%SCRIPT_DIR%start-borrowhub.bat"
exit /b %ERRORLEVEL%
