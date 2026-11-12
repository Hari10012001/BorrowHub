@echo off
setlocal enabledelayedexpansion

REM ============================================================================
REM BorrowHub - Open Frontend in Default Browser
REM ============================================================================

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "PROJECT_ROOT=%%~fI"

echo ============================================================================
echo [BorrowHub] Launching Web Application in Default Browser
echo ============================================================================

set "FRONTEND_URL=http://localhost:5173"

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "try { $resp = Invoke-WebRequest -Uri 'http://localhost:5173' -UseBasicParsing -TimeoutSec 3; exit 0 } catch { exit 1 }"

if %ERRORLEVEL% EQU 0 (
    echo [BorrowHub] Frontend is active. Opening %FRONTEND_URL% in default browser...
    start "" "%FRONTEND_URL%"
    exit /b 0
) else (
    echo [BorrowHub WARNING] Frontend server is not responding at %FRONTEND_URL%.
    echo Please start the platform first by running:
    echo   scripts\start-borrowhub.bat
    exit /b 1
)
