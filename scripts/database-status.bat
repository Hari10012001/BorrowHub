@echo off
setlocal enabledelayedexpansion

REM ============================================================================
REM BorrowHub - MySQL Database Status Inspection
REM ============================================================================

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "PROJECT_ROOT=%%~fI"

echo ============================================================================
echo [BorrowHub] Inspecting Database Status
echo ============================================================================

set "PORT=3306"
set "DB_NAME=borrowhub_db"

echo Configured Port   : %PORT%
echo Database Name     : %DB_NAME%

REM Check port status
netstat -ano | findstr /R /C:":%PORT% .*LISTENING" >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo Service Status    : NOT RUNNING
    echo [BorrowHub ERROR] MySQL is not responding on port %PORT%.
    exit /b 1
)

echo Service Status    : ACTIVE (Listening on port %PORT%)

if exist "%SCRIPT_DIR%db-status.ps1" (
    powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%db-status.ps1"
)

echo ============================================================================
exit /b 0
