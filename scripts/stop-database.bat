@echo off
setlocal enabledelayedexpansion

REM ============================================================================
REM BorrowHub - Safe MySQL Database Service Shutdown
REM ============================================================================

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "PROJECT_ROOT=%%~fI"
set "RUNTIME_DIR=%PROJECT_ROOT%\runtime"
set "PID_FILE=%RUNTIME_DIR%\mysql.pid"

echo ============================================================================
echo [BorrowHub] Initiating Safe Database Shutdown...
echo ============================================================================

REM Check if port 3306 is listening
netstat -ano | findstr /R /C:":3306 .*LISTENING" >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [BorrowHub] No active process listening on port 3306.
    if exist "%PID_FILE%" del /F /Q "%PID_FILE%" >nul 2>&1
    exit /b 0
)

REM Safe termination with CommandLine inspection to avoid stopping unrelated MySQL services
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$pidFile = $env:PID_FILE; " ^
    "$stopped = $false; " ^
    "if (Test-Path $pidFile) { " ^
    "    $myPid = (Get-Content $pidFile -ErrorAction SilentlyContinue).Trim(); " ^
    "    if ($myPid -match '^\d+$') { " ^
    "        $proc = Get-CimInstance Win32_Process -Filter ('ProcessId = ' + $myPid) -ErrorAction SilentlyContinue; " ^
    "        if ($proc) { " ^
    "            $cmd = if ($proc.CommandLine) { $proc.CommandLine } else { '' }; " ^
    "            if ($cmd -match 'borrowhub_mysql_data') { " ^
    "                Write-Host ('  Stopping verified BorrowHub mysqld daemon (PID: ' + $proc.ProcessId + ')'); " ^
    "                Stop-Process -Id $proc.ProcessId -Force -ErrorAction SilentlyContinue; " ^
    "                $stopped = $true; " ^
    "            } else { " ^
    "                Write-Host ('  [SAFETY] Process with PID ' + $myPid + ' does NOT match BorrowHub database data directory. Preserving process.') -ForegroundColor Yellow; " ^
    "            } " ^
    "        } else { " ^
    "            Write-Host ('  [SAFETY] Stale mysql.pid detected. Process no longer active.') -ForegroundColor Yellow; " ^
    "        } " ^
    "    } " ^
    "    Remove-Item $pidFile -Force -ErrorAction SilentlyContinue; " ^
    "} " ^
    "if (-not $stopped) { " ^
    "    $conn = Get-NetTCPConnection -LocalPort 3306 -State Listen -ErrorAction SilentlyContinue; " ^
    "    if ($conn) { " ^
    "        $proc = Get-CimInstance Win32_Process -Filter ('ProcessId = ' + $conn.OwningProcess) -ErrorAction SilentlyContinue; " ^
    "        if ($proc) { " ^
    "            $cmd = if ($proc.CommandLine) { $proc.CommandLine } else { '' }; " ^
    "            if ($cmd -match 'borrowhub_mysql_data') { " ^
    "                Write-Host ('  Stopping verified BorrowHub mysqld instance on port 3306 (PID: ' + $proc.ProcessId + ')'); " ^
    "                Stop-Process -Id $proc.ProcessId -Force -ErrorAction SilentlyContinue; " ^
    "            } else { " ^
    "                Write-Host ('  [SAFETY] Port 3306 is maintained by an external MySQL instance or Windows Service (PID: ' + $proc.ProcessId + '). Refusing to terminate unrelated database service.') -ForegroundColor Yellow; " ^
    "            } " ^
    "        } " ^
    "    } " ^
    "}"

ping -n 3 127.0.0.1 >nul
netstat -ano | findstr /R /C:":3306 .*LISTENING" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [BorrowHub NOTE] Port 3306 remains active (external/system MySQL service preserved).
    exit /b 0
) else (
    echo [BorrowHub] BorrowHub MySQL daemon stopped successfully. Port 3306 released.
    exit /b 0
)
