@echo off
setlocal enabledelayedexpansion

REM ============================================================================
REM BorrowHub - Graceful Application Shutdown
REM ============================================================================

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "PROJECT_ROOT=%%~fI"
set "RUNTIME_DIR=%PROJECT_ROOT%\runtime"
set "BACKEND_PID_FILE=%RUNTIME_DIR%\backend.pid"
set "FRONTEND_PID_FILE=%RUNTIME_DIR%\frontend.pid"

echo ============================================================================
echo [BorrowHub] Stopping BorrowHub Application Services...
echo ============================================================================

REM 1. Stop Frontend Service (Port 5173 or PID file)
echo [1/2] Terminating Frontend Service...
if exist "%FRONTEND_PID_FILE%" (
    set /p FE_PID=<"%FRONTEND_PID_FILE%"
    set "FE_PID=!FE_PID: =!"
    if not "!FE_PID!"=="" (
        echo   Stopping recorded Frontend PID: !FE_PID!
        taskkill /PID !FE_PID! /T >nul 2>&1
        ping -n 2 127.0.0.1 >nul
        taskkill /PID !FE_PID! /F /T >nul 2>&1
    )
    del /F /Q "%FRONTEND_PID_FILE%" >nul 2>&1
)

REM Fallback port-based targeted termination for Port 5173
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$conn = Get-NetTCPConnection -LocalPort 5173 -State Listen -ErrorAction SilentlyContinue; " ^
    "if ($conn) { " ^
    "    $p = Get-Process -Id $conn.OwningProcess -ErrorAction SilentlyContinue; " ^
    "    if ($p -and ($p.ProcessName -match 'node|npm|cmd')) { " ^
    "        Write-Host ('  Stopping process on port 5173 (PID: ' + $p.Id + ' - ' + $p.ProcessName + ')'); " ^
    "        Stop-Process -Id $p.Id -Force -ErrorAction SilentlyContinue; " ^
    "    } " ^
    "}"

REM 2. Stop Backend Service (Port 8080 or PID file)
echo.
echo [2/2] Terminating Backend Service...
if exist "%BACKEND_PID_FILE%" (
    set /p BE_PID=<"%BACKEND_PID_FILE%"
    set "BE_PID=!BE_PID: =!"
    if not "!BE_PID!"=="" (
        echo   Stopping recorded Backend PID: !BE_PID!
        taskkill /PID !BE_PID! /T >nul 2>&1
        ping -n 3 127.0.0.1 >nul
        taskkill /PID !BE_PID! /F /T >nul 2>&1
    )
    del /F /Q "%BACKEND_PID_FILE%" >nul 2>&1
)

REM Fallback port-based targeted termination for Port 8080
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$conn = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue; " ^
    "if ($conn) { " ^
    "    $p = Get-Process -Id $conn.OwningProcess -ErrorAction SilentlyContinue; " ^
    "    if ($p -and ($p.ProcessName -match 'java|javaw')) { " ^
    "        Write-Host ('  Stopping BorrowHub backend on port 8080 (PID: ' + $p.Id + ')'); " ^
    "        Stop-Process -Id $p.Id -Force -ErrorAction SilentlyContinue; " ^
    "    } " ^
    "}"

ping -n 3 127.0.0.1 >nul

REM Verify Port Release
echo.
set "PORTS_RELEASED=1"
netstat -ano | findstr /R /C:":8080 .*LISTENING" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [BorrowHub WARNING] Port 8080 is still in use!
    set "PORTS_RELEASED=0"
) else (
    echo [BorrowHub] Port 8080 released.
)

netstat -ano | findstr /R /C:":5173 .*LISTENING" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [BorrowHub WARNING] Port 5173 is still in use!
    set "PORTS_RELEASED=0"
) else (
    echo [BorrowHub] Port 5173 released.
)

echo ============================================================================
if %PORTS_RELEASED% EQU 1 (
    echo [BorrowHub] All BorrowHub application services stopped successfully.
    exit /b 0
) else (
    echo [BorrowHub ERROR] Some ports could not be released.
    exit /b 1
)
