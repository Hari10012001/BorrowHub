@echo off
setlocal enabledelayedexpansion

REM ============================================================================
REM BorrowHub - Open Interactive Database Shell
REM ============================================================================

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "PROJECT_ROOT=%%~fI"

echo ============================================================================
echo [BorrowHub] Interactive Database Console Access
echo ============================================================================

set "MYSQL_EXE="
if exist "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" (
    set "MYSQL_EXE=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
) else (
    where mysql >nul 2>&1
    if %ERRORLEVEL% EQU 0 (
        for /f "delims=" %%I in ('where mysql') do set "MYSQL_EXE=%%I"
    )
)

if "%MYSQL_EXE%"=="" (
    echo [BorrowHub] MySQL CLI client was not found in standard paths.
    echo.
    echo Connection Details for GUI Clients (e.g., DBeaver, Workbench):
    echo   Host     : localhost
    echo   Port     : 3306
    echo   Database : borrowhub_db
    echo   User     : borrowhub_user
    echo.
    echo To configure MySQL CLI login-path securely:
    echo   mysql_config_editor set --login-path=borrowhub --host=localhost --user=borrowhub_user --password
    pause
    exit /b 1
)

echo Connecting to database 'borrowhub_db' on localhost:3306...
echo You will be prompted for password (default development: BorrowHub@2026).
echo.
"%MYSQL_EXE%" -u borrowhub_user -p -h localhost -P 3306 borrowhub_db

exit /b %ERRORLEVEL%
