# BorrowHub — Windows BAT Automation Guide

This guide documents the behavior, usage, process management, and operational guarantees of all Windows batch scripts located in the `scripts/` directory.

---

## 1. Script Architecture & Design Principles

All scripts adhere to the following operational standards:
1. **Self-Locating:** Resolves the project root dynamically (`%~dp0..`), functioning identically whether double-clicked from File Explorer or run from Command Prompt / PowerShell.
2. **Process Ownership & PID Tracking:** Process IDs are tracked in `runtime/*.pid` to prevent terminating unrelated background software.
3. **Dedicated Logging:** Standard output and error streams are redirected to `logs/` for post-mortem analysis.
4. **Non-Zero Exit Codes:** Returns meaningful exit codes (`0` for success, non-zero for failures) for CI/CD integration.
5. **Port Contention Protection:** Checks port availability prior to spawning services to prevent duplicate instances.

---

## 2. Master Automation Scripts Catalog

### 1. `scripts\start-borrowhub.bat`
- **Purpose:** Full platform startup.
- **Workflow:**
  1. Verifies Java, Node.js, and npm toolchains.
  2. Verifies MySQL on port 3306 (starts server via `start-database.bat` if offline).
  3. Verifies backend fat JAR (builds with Maven if missing) and spawns process, saving PID to `runtime\backend.pid`.
  4. Polls `/actuator/health` until status is `UP`.
  5. Spawns frontend dev server, saving PID to `runtime\frontend.pid`.
  6. Polls `http://localhost:5173` until HTTP 200.
  7. Opens default browser to `http://localhost:5173`.

### 2. `scripts\stop-borrowhub.bat`
- **Purpose:** Graceful application shutdown.
- **Workflow:**
  1. Inspects `runtime\frontend.pid` and terminates tracked frontend process.
  2. Inspects `runtime\backend.pid` and terminates tracked backend process.
  3. Verifies ports 8080 and 5173 are released.
  4. Deletes stale PID files.

### 3. `scripts\restart-borrowhub.bat`
- **Purpose:** Clean service restart.
- **Workflow:** Calls `stop-borrowhub.bat`, waits 3 seconds, then calls `start-borrowhub.bat`.

### 4. `scripts\start-database.bat`
- **Purpose:** Starts MySQL 8.0 server.
- **Workflow:** Checks if port 3306 is already active; if not, checks Windows Services (`MySQL80`, `MySQL`) or launches local `mysqld.exe` with `--default-time-zone="+05:30"`.

### 5. `scripts\stop-database.bat`
- **Purpose:** Stops MySQL server.
- **Workflow:** Checks tracked PID in `runtime\mysql.pid` or Windows service, gracefully terminates process.

### 6. `scripts\database-status.bat`
- **Purpose:** Database health inspection.
- **Workflow:** Inspects MySQL port 3306, version, total table count, and latest Flyway migration status without exposing credentials.

### 7. `scripts\open-database.bat`
- **Purpose:** Interactive MySQL shell.
- **Workflow:** Launches interactive `mysql.exe` CLI prompt for `borrowhub_db` with interactive password entry.

### 8. `scripts\open-borrowhub.bat`
- **Purpose:** Browser application launcher.
- **Workflow:** Probes `http://localhost:5173` and opens default browser if reachable.

### 9. `scripts\health-check.bat`
- **Purpose:** System-wide multi-tier health probe.
- **Workflow:** Validates toolchains, MySQL 3306, Backend 8080 (`/actuator/health`), Public REST API (`/api/resources`), and Frontend 5173. Returns 0 if healthy, 1 if unhealthy.

### 10. `scripts\run-backend-tests.bat`
- **Purpose:** Executes JUnit 5 / Spring Boot architecture tests (`mvn test`).

### 11. `scripts\run-frontend-tests.bat`
- **Purpose:** Executes Vitest unit/component tests and Vite production build verification.

### 12. `scripts\run-e2e-tests.bat`
- **Purpose:** Executes Playwright headless browser end-to-end tests across all user journeys.

### 13. `scripts\run-full-qa.bat`
- **Purpose:** Master QA orchestrator running all 12 QA pipeline stages and generating a timestamped Markdown report in `qa\reports\`.
