@echo off
setlocal enabledelayedexpansion

REM ============================================================================
REM BorrowHub - Stop MySQL Database Service
REM ============================================================================

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "PROJECT_ROOT=%%~fI"
set "RUNTIME_DIR=%PROJECT_ROOT%\runtime"
set "PID_FILE=%RUNTIME_DIR%\mysql.pid"

echo ============================================================================
echo [BorrowHub] Stopping MySQL Database Service...
echo ============================================================================

REM Check if port 3306 is listening
netstat -ano | findstr /R /C:":3306 .*LISTENING" >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [BorrowHub] No active process listening on port 3306.
    if exist "%PID_FILE%" del /F /Q "%PID_FILE%" >nul 2>&1
    exit /b 0
)

REM If tracked by PID file, stop that specific PID
if exist "%PID_FILE%" (
    set /p MYSQL_PID=<"%PID_FILE%"
    set "MYSQL_PID=!MYSQL_PID: =!"
    if not "!MYSQL_PID!"=="" (
        echo [BorrowHub] Stopping tracked MySQL PID: !MYSQL_PID!...
        taskkill /PID !MYSQL_PID! /T >nul 2>&1
        ping -n 3 127.0.0.1 >nul
        taskkill /PID !MYSQL_PID! /F /T >nul 2>&1
        del /F /Q "%PID_FILE%" >nul 2>&1
    )
)

REM Check Windows Services
sc query "MySQL80" | findstr "RUNNING" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [BorrowHub] Stopping Windows Service 'MySQL80'...
    net stop "MySQL80" >nul 2>&1
)

sc query "MySQL" | findstr "RUNNING" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [BorrowHub] Stopping Windows Service 'MySQL'...
    net stop "MySQL" >nul 2>&1
)

ping -n 3 127.0.0.1 >nul
netstat -ano | findstr /R /C:":3306 .*LISTENING" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [BorrowHub WARNING] Port 3306 is still active. Another MySQL service or daemon may be active.
    exit /b 1
) else (
    echo [BorrowHub] MySQL Database stopped successfully. Port 3306 released.
    exit /b 0
)
