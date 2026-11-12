# BorrowHub (APPJFS12) — Windows Automation Script Safety Audit Report

**Document Version:** 1.0.0  
**Audit Date:** 2026-11-12  
**Auditor:** Principal Java Architect & Windows DevOps Specialist  
**Target Directory:** `scripts/`  
**Overall Script Safety Verdict:** **VERIFIED SAFE (All 13 Scripts Hardened)**

---

## 1. Script Inventory & Architecture Overview

The BorrowHub Windows automation layer provides 13 batch scripts (`.bat`) and accompanying PowerShell support scripts (`.ps1`) designed for reliable single-command startup, shutdown, health inspection, testing, and lifecycle management on Windows development workstations.

| Script Name | Target Service / Action | Double-Click Safe | Quoted Paths | Process Check Method |
|---|---|:---:|:---:|---|
| `start-borrowhub.bat` | Backend + Frontend + DB | Yes | Yes | Port check + Health polling + CIM inspection |
| `stop-borrowhub.bat` | Backend + Frontend | Yes | Yes | CIM/WMI `CommandLine` pattern match |
| `restart-borrowhub.bat` | Orderly Cooldown Restart | Yes | Yes | Cooldown pause + Chained script invocation |
| `start-database.bat` | MySQL 8.0 Standalone | Yes | Yes | Port check + Service check + Binary fallback |
| `stop-database.bat` | MySQL 8.0 Standalone | Yes | Yes | CIM/WMI `CommandLine` datadir match |
| `database-status.bat` | MySQL 8.0 Status | Yes | Yes | Port check + PowerShell SQL status probe |
| `health-check.bat` | Multi-tier System Check | Yes | Yes | Port probing + REST health interrogation |
| `open-borrowhub.bat` | Default Web Browser | Yes | Yes | HTTP 200 verification prior to launch |
| `open-database.bat` | Interactive MySQL Shell | Yes | Yes | Path lookup + Masked interactive password prompt |
| `run-backend-tests.bat` | Maven / JUnit 5 Tests | Yes | Yes | DB availability check + Maven execution |
| `run-frontend-tests.bat`| Vitest + Vite Build | Yes | Yes | Vitest execution + Production bundle compilation |
| `run-e2e-tests.bat` | Playwright Browser Tests | Yes | Yes | Service readiness check + Playwright execution |
| `run-full-qa.bat` | 12-Stage QA Pipeline | Yes | Yes | PowerShell Master QA Orchestrator invocation |

---

## 2. Process Termination Safety Audit

### 2.1 The Vulnerability of Blind Process Kills
In standard Windows batch scripting, developers frequently employ:
```cmd
taskkill /F /PID %PID%
```
or
```cmd
taskkill /F /IM java.exe
taskkill /F /IM node.exe
```
**Why this is dangerous:**
1. Blindly killing by image name (`java.exe` or `node.exe`) destroys all other developer workloads running on the machine (e.g., IDE language servers, build tools, other projects).
2. PIDs are recycled by Windows immediately upon process death. If a service terminates abruptly, its PID in `runtime/*.pid` may subsequently belong to an arbitrary system or user process.

### 2.2 Re-Engineered Safety Enforcement
In BorrowHub, `stop-borrowhub.bat` and `stop-database.bat` now invoke PowerShell with CIM instance inspection:
```powershell
$proc = Get-CimInstance Win32_Process -Filter "ProcessId = $pidNum"
if ($proc -and ($proc.CommandLine -match "borrowhub-backend|BorrowHubApplication")) {
    Stop-Process -Id $pidNum -Force
} else {
    Write-Host "PID does not match BorrowHub command line. Skipping termination."
}
```
**Safety Matrix:**

| Process Target | Verification Pattern | Unverified Fallback Behavior |
|---|---|---|
| Frontend (Node / Vite) | `borrowhub-frontend` OR `vite` | Log warning, remove stale PID, skip kill |
| Backend (Java / Spring) | `borrowhub-backend` OR `BorrowHubApplication` | Log warning, remove stale PID, skip kill |
| Database (MySQL) | `borrowhub_mysql_data` | Refuse kill; preserve system MySQL services |

### 2.3 Port Conflict Guarding
If an unverified process occupies port 8080 or 5173, `stop-borrowhub.bat` inspects the occupying PID's `CommandLine`. If the command line does not belong to BorrowHub, the script refuses to kill the process and displays:
```
[BorrowHub WARNING] Port 8080 is occupied by an unverified process (PID: XXXX).
Process will NOT be terminated. Please inspect PID manually.
```

---

## 3. Startup & Service Orchestration Safety

### 3.1 Duplicate Service Prevention
`start-borrowhub.bat` and `start-database.bat` inspect active listening ports before attempting to launch new processes:
- Checks port 3306: If already active, MySQL launch is skipped and execution proceeds directly to connection verification.
- Checks port 8080: If already active, backend launch is skipped.
- Checks port 5173: If already active, frontend launch is skipped.

### 3.2 Readiness-Gated Browser Launch
Previously, startup scripts would open the browser immediately after launching node, resulting in HTTP `ERR_CONNECTION_REFUSED` while Vite was building modules.
`start-borrowhub.bat` and `open-borrowhub.bat` now poll the frontend URL:
```cmd
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "try { $resp = Invoke-WebRequest -Uri 'http://localhost:5173' -UseBasicParsing -TimeoutSec 2; if ($resp.StatusCode -eq 200) { exit 0 } else { exit 1 } } catch { exit 1 }"
```
Only when the HTTP status code returns `200` does the script execute `start "" "http://localhost:5173"`. If the server does not become healthy within 30 seconds, the browser launch is cancelled and the script exits with an error code.

### 3.3 Elimination of False Zero-Downtime Claims
Single-instance JVM restarts require process termination, JVM teardown, port release, and JVM cold boot. `restart-borrowhub.bat` accurately labels this process as a "Planned Service Cooldown", providing a 3-second cooling interval to guarantee complete TCP socket cleanup before initiating the new processes.

---

## 4. Credential & Environment Protection

1. **Masked Password Prompts:**  
   `open-database.bat` connects to MySQL via `mysql -u borrowhub_user -p` which triggers native console password masking, ensuring credentials never appear in terminal logs, screen recordings, or command history.
2. **Environment Variable Overrides:**  
   Scripts prioritize system environment variables (`DB_PASSWORD`, `JWT_SECRET`) when set, falling back to development defaults only in local environments.
3. **No Hardcoded Production Secrets:**  
   All default passwords and secrets are explicitly marked for local development testing only.

---

## 5. Execution Environment & Path Handling

- **Space-Safe Quoting:** Every path reference uses `%~dp0`, `%PROJECT_ROOT%`, and `%SCRIPT_DIR%` wrapped in double quotes (e.g. `"%PROJECT_ROOT%\runtime\backend.pid"`), preventing path truncation on Windows directories with spaces (such as `D:\HARIHARAN P\...`).
- **Double-Click Compatibility:** All batch scripts include `setlocal enabledelayedexpansion` and pause/exit blocks with explicit error codes, functioning reliably both when executed from a command terminal and when double-clicked in Windows File Explorer.

---

## 6. Audit Conclusion

All 13 batch automation scripts comply with production engineering standards for Windows environments. Unsafe blind termination, race conditions, false downtime claims, and credential exposure have been eliminated.
