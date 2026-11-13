@echo off
setlocal enabledelayedexpansion

REM ============================================================================
REM BorrowHub - Start MySQL Database Service
REM ============================================================================

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "PROJECT_ROOT=%%~fI"
set "RUNTIME_DIR=%PROJECT_ROOT%\runtime"
set "LOGS_DIR=%PROJECT_ROOT%\logs"
set "PID_FILE=%RUNTIME_DIR%\mysql.pid"
set "LOG_FILE=%LOGS_DIR%\mysql.log"

if not exist "%RUNTIME_DIR%" mkdir "%RUNTIME_DIR%"
if not exist "%LOGS_DIR%" mkdir "%LOGS_DIR%"

echo ============================================================================
echo [BorrowHub] Checking MySQL Database Service...
echo ============================================================================

REM Check if port 3306 is already active
netstat -ano | findstr /R /C:":3306 .*LISTENING" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [BorrowHub] MySQL is already listening on port 3306.
    goto :VERIFY
)

REM Try Windows Service first
sc query "MySQL80" >nul 2>&1
if !ERRORLEVEL! EQU 0 (
    echo [BorrowHub] Attempting to start Windows Service 'MySQL80'...
    net start "MySQL80" >nul 2>&1
    ping -n 3 127.0.0.1 >nul
    netstat -ano | findstr /R /C:":3306 .*LISTENING" >nul 2>&1
    if !ERRORLEVEL! EQU 0 (
        echo [BorrowHub] MySQL Service started successfully.
        goto :VERIFY
    )
)

sc query "MySQL" >nul 2>&1
if !ERRORLEVEL! EQU 0 (
    echo [BorrowHub] Attempting to start Windows Service 'MySQL'...
    net start "MySQL" >nul 2>&1
    ping -n 3 127.0.0.1 >nul
    netstat -ano | findstr /R /C:":3306 .*LISTENING" >nul 2>&1
    if !ERRORLEVEL! EQU 0 (
        echo [BorrowHub] MySQL Service started successfully.
        goto :VERIFY
    )
)

REM Fallback to local mysqld binary
set "MYSQLD_EXE="
if exist "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqld.exe" (
    set "MYSQLD_EXE=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqld.exe"
) else (
    where mysqld >nul 2>&1
    if %ERRORLEVEL% EQU 0 (
        for /f "delims=" %%I in ('where mysqld') do set "MYSQLD_EXE=%%I"
    )
)

if "%MYSQLD_EXE%"=="" (
    echo [BorrowHub ERROR] mysqld.exe could not be located on the system.
    echo Please ensure MySQL 8.0 is installed and in PATH or running as a service.
    exit /b 1
)

set "DATA_DIR=%USERPROFILE%\borrowhub_mysql_data"
if not exist "%DATA_DIR%" (
    if exist "%PROJECT_ROOT%\borrowhub_mysql_data" (
        set "DATA_DIR=%PROJECT_ROOT%\borrowhub_mysql_data"
    )
)

echo [BorrowHub] Starting MySQL Server via binary...
echo Executable: %MYSQLD_EXE%
echo Data Directory: %DATA_DIR%

start /B "BorrowHub-MySQL" "%MYSQLD_EXE%" --datadir="%DATA_DIR%" --port=3306 --default-time-zone="+05:30" --console > "%LOG_FILE%" 2>&1

REM Wait for port 3306 to become ready
echo [BorrowHub] Waiting for MySQL to accept connections on port 3306...
set /a WAIT_COUNT=0
:WAIT_MYSQL
ping -n 2 127.0.0.1 >nul
netstat -ano | findstr /R /C:":3306 .*LISTENING" >nul 2>&1
if %ERRORLEVEL% EQU 0 goto :RECORD_PID
set /a WAIT_COUNT+=1
if %WAIT_COUNT% GEQ 20 (
    echo [BorrowHub ERROR] MySQL failed to start within 20 seconds.
    echo Check log output in %LOG_FILE%
    exit /b 1
)
goto :WAIT_MYSQL

:RECORD_PID
for /f "tokens=5" %%P in ('netstat -ano ^| findstr /R /C:":3306 .*LISTENING"') do (
    echo %%P > "%PID_FILE%"
    goto :VERIFY
)

:VERIFY
echo [BorrowHub] MySQL Database is ACTIVE and listening on port 3306.
echo [BorrowHub] Timezone configured: +05:30 (Asia/Kolkata)
exit /b 0
