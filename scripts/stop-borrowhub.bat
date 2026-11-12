@echo off
setlocal enabledelayedexpansion

REM ============================================================================
REM BorrowHub - Graceful & Safe Application Shutdown
REM ============================================================================

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "PROJECT_ROOT=%%~fI"
set "RUNTIME_DIR=%PROJECT_ROOT%\runtime"
set "BACKEND_PID_FILE=%RUNTIME_DIR%\backend.pid"
set "FRONTEND_PID_FILE=%RUNTIME_DIR%\frontend.pid"

echo ============================================================================
echo [BorrowHub] Initiating Safe Application Shutdown...
echo ============================================================================

REM 1. Stop Frontend Service with Ownership & Stale-PID Verification
echo [1/2] Terminating Frontend Service (Port 5173)...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$projectRoot = $env:PROJECT_ROOT; " ^
    "$pidFile = $env:FRONTEND_PID_FILE; " ^
    "if (Test-Path $pidFile) { " ^
    "    $fePid = (Get-Content $pidFile -ErrorAction SilentlyContinue).Trim(); " ^
    "    if ($fePid -match '^\d+$') { " ^
    "        $proc = Get-CimInstance Win32_Process -Filter ('ProcessId = ' + $fePid) -ErrorAction SilentlyContinue; " ^
    "        if ($proc) { " ^
    "            $cmd = if ($proc.CommandLine) { $proc.CommandLine } else { '' }; " ^
    "            if ($cmd -match 'borrowhub-frontend|vite' -or $cmd.Contains($projectRoot)) { " ^
    "                Write-Host ('  Stopping verified BorrowHub frontend PID: ' + $proc.ProcessId); " ^
    "                Stop-Process -Id $proc.ProcessId -Force -ErrorAction SilentlyContinue; " ^
    "            } else { " ^
    "                Write-Host ('  [SAFETY] Stale frontend PID ' + $fePid + ' detected (does not match BorrowHub). Discarding PID file without killing process.') -ForegroundColor Yellow; " ^
    "            } " ^
    "        } " ^
    "    } " ^
    "    Remove-Item $pidFile -Force -ErrorAction SilentlyContinue; " ^
    "} " ^
    "$conn = Get-NetTCPConnection -LocalPort 5173 -State Listen -ErrorAction SilentlyContinue; " ^
    "if ($conn) { " ^
    "    $proc = Get-CimInstance Win32_Process -Filter ('ProcessId = ' + $conn.OwningProcess) -ErrorAction SilentlyContinue; " ^
    "    if ($proc) { " ^
    "        $cmd = if ($proc.CommandLine) { $proc.CommandLine } else { '' }; " ^
    "        if ($cmd -match 'borrowhub-frontend|vite|npm|node' -and ($cmd.Contains($projectRoot) -or $proc.Name -match 'node|cmd')) { " ^
    "            Write-Host ('  Stopping verified process listening on 5173 (PID: ' + $proc.ProcessId + ')'); " ^
    "            Stop-Process -Id $proc.ProcessId -Force -ErrorAction SilentlyContinue; " ^
    "        } else { " ^
    "            Write-Host ('  [SAFETY] Port 5173 is occupied by PID ' + $proc.ProcessId + ' (' + $proc.Name + '), but CommandLine does not match BorrowHub. Preserving process.') -ForegroundColor Yellow; " ^
    "        } " ^
    "    } " ^
    "}"

REM 2. Stop Backend Service with Ownership & Stale-PID Verification
echo.
echo [2/2] Terminating Backend Service (Port 8080)...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$projectRoot = $env:PROJECT_ROOT; " ^
    "$pidFile = $env:BACKEND_PID_FILE; " ^
    "if (Test-Path $pidFile) { " ^
    "    $bePid = (Get-Content $pidFile -ErrorAction SilentlyContinue).Trim(); " ^
    "    if ($bePid -match '^\d+$') { " ^
    "        $proc = Get-CimInstance Win32_Process -Filter ('ProcessId = ' + $bePid) -ErrorAction SilentlyContinue; " ^
    "        if ($proc) { " ^
    "            $cmd = if ($proc.CommandLine) { $proc.CommandLine } else { '' }; " ^
    "            if ($cmd -match 'borrowhub-backend|BorrowHubApplication' -or $cmd.Contains($projectRoot)) { " ^
    "                Write-Host ('  Stopping verified BorrowHub backend PID: ' + $proc.ProcessId); " ^
    "                Stop-Process -Id $proc.ProcessId -Force -ErrorAction SilentlyContinue; " ^
    "            } else { " ^
    "                Write-Host ('  [SAFETY] Stale backend PID ' + $bePid + ' detected (does not match BorrowHub). Discarding PID file without killing process.') -ForegroundColor Yellow; " ^
    "            } " ^
    "        } " ^
    "    } " ^
    "    Remove-Item $pidFile -Force -ErrorAction SilentlyContinue; " ^
    "} " ^
    "$conn = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue; " ^
    "if ($conn) { " ^
    "    $proc = Get-CimInstance Win32_Process -Filter ('ProcessId = ' + $conn.OwningProcess) -ErrorAction SilentlyContinue; " ^
    "    if ($proc) { " ^
    "        $cmd = if ($proc.CommandLine) { $proc.CommandLine } else { '' }; " ^
    "        if ($cmd -match 'borrowhub-backend|BorrowHubApplication' -or $cmd.Contains($projectRoot)) { " ^
    "            Write-Host ('  Stopping verified BorrowHub backend listening on 8080 (PID: ' + $proc.ProcessId + ')'); " ^
    "            Stop-Process -Id $proc.ProcessId -Force -ErrorAction SilentlyContinue; " ^
    "        } else { " ^
    "            Write-Host ('  [SAFETY] Port 8080 is occupied by PID ' + $proc.ProcessId + ' (' + $proc.Name + '), but CommandLine does not match BorrowHub. Preserving process.') -ForegroundColor Yellow; " ^
    "        } " ^
    "    } " ^
    "}"

ping -n 3 127.0.0.1 >nul

REM Verify Port Release
echo.
set "PORTS_RELEASED=1"
netstat -ano | findstr /R /C:":8080 .*LISTENING" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [BorrowHub WARNING] Port 8080 is still in use by an unverified or lingering process.
    set "PORTS_RELEASED=0"
) else (
    echo [BorrowHub] Port 8080 released.
)

netstat -ano | findstr /R /C:":5173 .*LISTENING" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [BorrowHub WARNING] Port 5173 is still in use by an unverified or lingering process.
    set "PORTS_RELEASED=0"
) else (
    echo [BorrowHub] Port 5173 released.
)

echo ============================================================================
if %PORTS_RELEASED% EQU 1 (
    echo [BorrowHub] All verified BorrowHub application services stopped successfully.
    exit /b 0
) else (
    echo [BorrowHub NOTE] Shutdown routine completed. Some non-BorrowHub listeners may remain.
    exit /b 0
)
