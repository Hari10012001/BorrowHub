@echo off
setlocal enabledelayedexpansion

REM ============================================================================
REM BorrowHub - Master Application Startup Script
REM ============================================================================

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "PROJECT_ROOT=%%~fI"
set "RUNTIME_DIR=%PROJECT_ROOT%\runtime"
set "LOGS_DIR=%PROJECT_ROOT%\logs"
set "BACKEND_PID_FILE=%RUNTIME_DIR%\backend.pid"
set "FRONTEND_PID_FILE=%RUNTIME_DIR%\frontend.pid"
set "BACKEND_LOG=%LOGS_DIR%\backend.log"
set "FRONTEND_LOG=%LOGS_DIR%\frontend.log"

if not exist "%RUNTIME_DIR%" mkdir "%RUNTIME_DIR%"
if not exist "%LOGS_DIR%" mkdir "%LOGS_DIR%"

echo ============================================================================
echo [BorrowHub] Initializing BorrowHub Full Stack Platform
echo ============================================================================

REM 1. Check Toolchain Requirements
echo [1/6] Checking Environment Toolchains...
where java >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [BorrowHub ERROR] Java 21 JDK is required but not found in PATH.
    pause
    exit /b 1
)

where node >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [BorrowHub ERROR] Node.js is required but not found in PATH.
    pause
    exit /b 1
)

where npm >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [BorrowHub ERROR] npm is required but not found in PATH.
    pause
    exit /b 1
)

echo   [PASS] Toolchains verified (Java, Node.js, npm).

REM 2. Check Database Service
echo.
echo [2/6] Checking MySQL Database Service (Port 3306)...
netstat -ano | findstr /R /C:":3306 .*LISTENING" >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo   MySQL is not listening on port 3306. Attempting automated startup...
    call "%SCRIPT_DIR%start-database.bat"
    if !ERRORLEVEL! NEQ 0 (
        echo [BorrowHub ERROR] Could not start MySQL database service.
        pause
        exit /b 1
    )
) else (
    echo   [PASS] MySQL is active and listening on port 3306.
)

REM 3. Check / Start Backend Service
echo.
echo [3/6] Checking Backend Spring Boot Service (Port 8080)...
netstat -ano | findstr /R /C:":8080 .*LISTENING" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
        "try { $h = Invoke-RestMethod -Uri 'http://localhost:8080/actuator/health' -TimeoutSec 2; if ($h.status -eq 'UP') { exit 0 } else { exit 1 } } catch { exit 1 }"
    if !ERRORLEVEL! EQU 0 (
        echo   [PASS] BorrowHub backend is already running and healthy on port 8080.
        goto :START_FRONTEND
    ) else (
        echo   [BorrowHub WARNING] Port 8080 is occupied by another process or unready backend.
        echo   Attempting to continue or probe readiness...
    )
) else (
    echo   Backend service is not running. Launching Spring Boot server...
    set "BACKEND_JAR=%PROJECT_ROOT%\borrowhub-backend\target\borrowhub-backend-1.0.0.jar"
    set "RUNTIME_JAR=%RUNTIME_DIR%\borrowhub-backend.jar"
    if not exist "!BACKEND_JAR!" (
        echo   [BorrowHub] Packaging backend fat JAR with Maven...
        cd /d "%PROJECT_ROOT%\borrowhub-backend"
        call mvn package -DskipTests
        if !ERRORLEVEL! NEQ 0 (
            echo [BorrowHub ERROR] Failed to package backend application.
            pause
            exit /b 1
        )
        cd /d "%PROJECT_ROOT%"
    )
    if not exist "!RUNTIME_JAR!" (
        copy /Y "!BACKEND_JAR!" "!RUNTIME_JAR!" >nul 2>&1
    ) else (
        copy /Y "!BACKEND_JAR!" "!RUNTIME_JAR!" >nul 2>&1
    )

    echo   Starting Backend JAR: !RUNTIME_JAR!
    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
        "$proc = Start-Process -FilePath 'java' -ArgumentList '-jar', ('\"' + $env:RUNTIME_JAR + '\"') -RedirectStandardOutput $env:BACKEND_LOG -RedirectStandardError ($env:LOGS_DIR + '\backend-error.log') -PassThru; [System.IO.File]::WriteAllText($env:BACKEND_PID_FILE, $proc.Id.ToString())"
)

REM 4. Wait for Backend Health
echo.
echo [4/6] Polling Backend Health Endpoint (http://localhost:8080/actuator/health)...
set /a BE_WAIT=0
:POLL_BACKEND
ping -n 3 127.0.0.1 >nul
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "try { $h = Invoke-RestMethod -Uri 'http://localhost:8080/actuator/health' -TimeoutSec 2; if ($h.status -eq 'UP') { exit 0 } else { exit 1 } } catch { exit 1 }"
if %ERRORLEVEL% EQU 0 (
    echo   [PASS] Backend Spring Boot application is UP and HEALTHY!
    goto :START_FRONTEND
)
set /a BE_WAIT+=2
if %BE_WAIT% GEQ 60 (
    echo [BorrowHub ERROR] Backend failed to report healthy status within 60 seconds.
    echo Please inspect log file: %BACKEND_LOG%
    pause
    exit /b 1
)
goto :POLL_BACKEND

REM 5. Check / Start Frontend Service
:START_FRONTEND
echo.
echo [5/6] Checking Frontend Web Server (Port 5173)...
netstat -ano | findstr /R /C:":5173 .*LISTENING" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
        "try { $f = Invoke-WebRequest -Uri 'http://localhost:5173' -UseBasicParsing -TimeoutSec 2; exit 0 } catch { exit 1 }"
    if !ERRORLEVEL! EQU 0 (
        echo   [PASS] Frontend server is already active and serving on port 5173.
        goto :LAUNCH_BROWSER
    ) else (
        echo   [BorrowHub WARNING] Port 5173 is listening but did not respond to HTTP probe.
    )
) else (
    echo   Frontend server is not running. Launching Vite dev server...
    cd /d "%PROJECT_ROOT%\borrowhub-frontend"
    if not exist "node_modules" (
        echo   Installing frontend dependencies...
        call npm install
    )
    cd /d "%PROJECT_ROOT%"
    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
        "$feDir = $env:PROJECT_ROOT + '\borrowhub-frontend'; $proc = Start-Process -FilePath 'cmd.exe' -ArgumentList '/c', 'npm run dev' -WorkingDirectory $feDir -RedirectStandardOutput $env:FRONTEND_LOG -RedirectStandardError ($env:LOGS_DIR + '\frontend-error.log') -PassThru; [System.IO.File]::WriteAllText($env:FRONTEND_PID_FILE, $proc.Id.ToString())"
)

REM Wait for frontend readiness
echo   Waiting for Frontend readiness on http://localhost:5173...
set /a FE_WAIT=0
:POLL_FRONTEND
ping -n 2 127.0.0.1 >nul
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "try { $f = Invoke-WebRequest -Uri 'http://localhost:5173' -UseBasicParsing -TimeoutSec 2; exit 0 } catch { exit 1 }"
if %ERRORLEVEL% EQU 0 (
    echo   [PASS] Frontend Web Application is ACTIVE and serving pages!
    goto :LAUNCH_BROWSER
)
set /a FE_WAIT+=1
if %FE_WAIT% GEQ 30 (
    echo [BorrowHub ERROR] Frontend server failed to report ready within 30 seconds.
    echo Browser launch aborted. Please inspect log file: %FRONTEND_LOG%
    pause
    exit /b 1
)
goto :POLL_FRONTEND

:LAUNCH_BROWSER
echo.
echo [6/6] Launching Application in Default Browser...
start "" "http://localhost:5173"

echo.
echo ============================================================================
echo [BorrowHub] Platform Successfully Started!
echo ============================================================================
echo Frontend Application : http://localhost:5173
echo Backend REST API     : http://localhost:8080
echo Health Check Status  : http://localhost:8080/actuator/health
echo Backend Logs         : %BACKEND_LOG%
echo Frontend Logs        : %FRONTEND_LOG%
echo.
echo Default Logins:
echo   Administrator : admin    / Admin@123
echo   Faculty       : faculty1 / Faculty@123
echo   Student       : student1 / Student@123
echo ============================================================================
exit /b 0
