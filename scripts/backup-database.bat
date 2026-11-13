@echo off
setlocal enabledelayedexpansion

REM ============================================================================
REM BorrowHub - Automated MySQL Database Backup Utility
REM ============================================================================

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "PROJECT_ROOT=%%~fI"
set "BACKUP_DIR=%PROJECT_ROOT%\database\backups"
set "ENV_FILE=%PROJECT_ROOT%\.env"

if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"

echo ============================================================================
echo [BorrowHub] Initiating Database Backup
echo ============================================================================

REM Load configuration from .env if present
set "DB_HOST=127.0.0.1"
set "DB_PORT=3306"
set "DB_USERNAME=borrowhub_user"
set "DB_PASSWORD="
set "DB_NAME=borrowhub_db"

if exist "%ENV_FILE%" (
    for /f "usebackq tokens=1,* delims==" %%A in ("%ENV_FILE%") do (
        set "KEY=%%A"
        set "VAL=%%B"
        for /f "tokens=* delims= " %%X in ("!KEY!") do set "KEY=%%X"
        for /f "tokens=* delims= " %%X in ("!VAL!") do set "VAL=%%X"
        if "!KEY!"=="DB_HOST" set "DB_HOST=!VAL!"
        if "!KEY!"=="DB_PORT" set "DB_PORT=!VAL!"
        if "!KEY!"=="DB_USERNAME" set "DB_USERNAME=!VAL!"
        if "!KEY!"=="DB_PASSWORD" set "DB_PASSWORD=!VAL!"
        if "!KEY!"=="DB_NAME" set "DB_NAME=!VAL!"
    )
)

if "%DB_PASSWORD%"=="" (
    echo [BorrowHub ERROR] DB_PASSWORD is not defined in environment or .env file.
    exit /b 1
)

REM Locate mysqldump executable
set "MYSQLDUMP_EXE="
if exist "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump.exe" (
    set "MYSQLDUMP_EXE=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump.exe"
) else (
    where mysqldump >nul 2>&1
    if !ERRORLEVEL! EQU 0 (
        for /f "delims=" %%I in ('where mysqldump') do set "MYSQLDUMP_EXE=%%I"
    )
)

if "%MYSQLDUMP_EXE%"=="" (
    echo [BorrowHub ERROR] mysqldump executable could not be found.
    exit /b 1
)

REM Generate timestamped filename
for /f "tokens=2 delims==" %%I in ('wmic os get localdatetime /value 2^>nul') do set "DT=%%I"
if "%DT%"=="" (
    set "STAMP=%DATE:~10,4%%DATE:~4,2%%DATE:~7,2%_%TIME:~0,2%%TIME:~3,2%%TIME:~6,2%"
    set "STAMP=!STAMP: =0!"
) else (
    set "STAMP=%DT:~0,8%_%DT:~8,6%"
)

set "BACKUP_FILE=%BACKUP_DIR%\borrowhub_backup_%STAMP%.sql"

echo Target Database : %DB_NAME%
echo Destination     : %BACKUP_FILE%

set "MYSQL_PWD=%DB_PASSWORD%"
"%MYSQLDUMP_EXE%" -h %DB_HOST% -P %DB_PORT% -u %DB_USERNAME% --single-transaction --quick --routines --triggers --hex-blob --default-character-set=utf8mb4 %DB_NAME% > "%BACKUP_FILE%" 2> "%PROJECT_ROOT%\logs\backup_err.log"
set "DUMP_EXIT=%ERRORLEVEL%"
set "MYSQL_PWD="

if %DUMP_EXIT% NEQ 0 (
    echo [BorrowHub ERROR] Backup failed with exit code %DUMP_EXIT%.
    if exist "%PROJECT_ROOT%\logs\backup_err.log" type "%PROJECT_ROOT%\logs\backup_err.log"
    exit /b %DUMP_EXIT%
)

if not exist "%BACKUP_FILE%" (
    echo [BorrowHub ERROR] Backup file was not generated.
    exit /b 1
)

for %%A in ("%BACKUP_FILE%") do set "FILE_SIZE=%%~zA"
echo [BorrowHub RESULT] BACKUP COMPLETED SUCCESSFULLY.
echo Backup Size     : %FILE_SIZE% bytes
echo ============================================================================
exit /b 0
