# BorrowHub (APPJFS12) — Master Defect Register & Resolution Log

**Document Version:** 1.1.0  
**Audit Date:** 2026-11-12  
**Auditor:** Principal Java Architect & Quality Assurance Lead  
**Scope:** Post-Implementation Verification & Final Defect Closure  

---

## Defect Summary Overview

| Defect ID | Severity | Category | Affected Component | Status | Verified In |
|---|---|---|---|---|---|
| **BUG-01** | High | Windows Automation | `scripts/stop-borrowhub.bat` | **RESOLVED** | CIM Process Verification |
| **BUG-02** | High | Windows Automation | `scripts/stop-database.bat` | **RESOLVED** | CIM DataDir Inspection |
| **BUG-03** | Medium | User Experience | `scripts/start-borrowhub.bat` | **RESOLVED** | HTTP 200 Polling Loop |
| **BUG-04** | Low | Documentation | `scripts/restart-borrowhub.bat` | **RESOLVED** | Documentation Cooldown Update |
| **BUG-05** | Medium | Security / Credential | `scripts/open-database.bat` | **RESOLVED** | Prompt Sanitization |
| **BUG-06** | Medium | Integration Tests | `qa/integration/test-all-flows.ps1`| **RESOLVED** | Dynamic ID Extraction |
| **BUG-07** | Medium | Database Integrity | `qa/integration/test-all-flows.ps1`| **RESOLVED** | QUANTITY Tracking Mode Resource |
| **BUG-08** | High | Application Core | `CirculationRetryOrchestrator.java` | **RESOLVED** | Spring ObjectMapper Injection |
| **BUG-09** | Low | Accessibility (a11y) | `borrowhub-frontend` Badges/Headers | **RESOLVED** | Axe-Core Playwright Scan |

**Summary Statistics:**
- **Total Defects Identified:** 9
- **Total Defects Resolved:** 9 (100%)
- **Active / Unresolved Defects:** 0

---

## Detailed Defect Records

---

### BUG-01: Windows Script Blind Process Termination via Unverified PID
- **Defect Title:** Script `stop-borrowhub.bat` kills arbitrary processes sharing PID or listening on port without command line validation.
- **Severity:** High
- **Category:** Windows Automation Safety
- **Affected Component:** `scripts/stop-borrowhub.bat`
- **Root Cause Analysis:**  
  The script read PIDs directly from `runtime/frontend.pid` and `runtime/backend.pid` and executed `taskkill /F /PID %PID%`. If a process crashed and the OS assigned that PID to another user application (e.g., Eclipse, Docker, browser), `stop-borrowhub.bat` would forcefully terminate the unrelated application.
- **Fix Applied:**  
  Implemented PowerShell CIM instance inspection `Get-CimInstance Win32_Process -Filter "ProcessId = $pidNum"`. The script checks whether `CommandLine` matches `borrowhub-frontend|vite` or `borrowhub-backend|BorrowHubApplication`. If no match is found, the kill is skipped and the stale PID file is safely deleted.
- **Verification Method:**  
  Tested with synthetic stale PID file containing an unrelated system process PID. Verified that the script refused termination and cleanly logged: `[BorrowHub] PID does not match BorrowHub command line. Skipping termination.`
- **Status:** **RESOLVED**

---

### BUG-02: Unconditional Host-Wide MySQL Service Termination
- **Defect Title:** `stop-database.bat` executed `net stop "MySQL80"` and `net stop "MySQL"` stopping unrelated database instances.
- **Severity:** High
- **Category:** Windows Automation Safety
- **Affected Component:** `scripts/stop-database.bat`
- **Root Cause Analysis:**  
  The shutdown script attempted to stop the Windows host services unconditionally, disrupting other local databases or developer projects running on the machine.
- **Fix Applied:**  
  Removed host-wide `net stop` calls. Script now targets solely the dedicated BorrowHub process by checking whether the process `CommandLine` contains `borrowhub_mysql_data`.
- **Verification Method:**  
  Verified that running `scripts/stop-database.bat` only terminates the standalone `mysqld.exe` process pointed to `borrowhub_mysql_data` and leaves host services unaffected.
- **Status:** **RESOLVED**

---

### BUG-03: Premature Browser Launch Prior to Verified Frontend Readiness
- **Defect Title:** `start-borrowhub.bat` launched default browser before frontend server was confirmed healthy.
- **Severity:** Medium
- **Category:** User Experience / Automation
- **Affected Component:** `scripts/start-borrowhub.bat`
- **Root Cause Analysis:**  
  The startup script polled for 10 iterations, but after reaching the maximum loop count, control simply fell through to `start "" "http://localhost:5173"`, causing a browser "Connection Refused" error page if Vite was delayed.
- **Fix Applied:**  
  Added strict readiness verification. If `%FE_WAIT% GEQ 30`, the script displays an explicit error message, aborts browser launch, pauses, and exits with code 1.
- **Verification Method:**  
  Tested startup sequence with frontend disabled. Verified browser was not opened and script exited with error code 1.
- **Status:** **RESOLVED**

---

### BUG-04: Misleading Zero-Downtime Rolling Restart Claim
- **Defect Title:** `restart-borrowhub.bat` advertised zero-downtime restarts on single-instance desktop architecture.
- **Severity:** Low
- **Category:** Documentation / Architectural Accuracy
- **Affected Component:** `scripts/restart-borrowhub.bat`
- **Root Cause Analysis:**  
  Zero-downtime rolling deployment requires a multi-instance load-balanced cluster. Restarting a single JVM backend on port 8080 necessarily incurs service downtime during process termination and restart.
- **Fix Applied:**  
  Re-titled script to "Planned Service Cooldown", documenting the expected 3-second service transition window.
- **Verification Method:**  
  Inspected script output and documentation.
- **Status:** **RESOLVED**

---

### BUG-05: Plaintext Password Display in Interactive Database Console Script
- **Defect Title:** `open-database.bat` printed database password in console output.
- **Severity:** Medium
- **Category:** Security / Credential Hygiene
- **Affected Component:** `scripts/open-database.bat`
- **Root Cause Analysis:**  
  The batch script contained an `echo` statement displaying the development password in plaintext on the terminal before launching `mysql.exe`.
- **Fix Applied:**  
  Removed plaintext credential output. Updated script to prompt the user securely using MySQL CLI's interactive `-p` masked password prompt.
- **Verification Method:**  
  Executed `scripts/open-database.bat` and confirmed zero plaintext passwords appeared in console output.
- **Status:** **RESOLVED**

---

### BUG-06: Hardcoded Borrower User ID in Automated Integration Flow
- **Defect Title:** Integration flow script assumed student user had ID 3.
- **Severity:** Medium
- **Category:** Test Rigor & Resilience
- **Affected Component:** `qa/integration/test-all-flows.ps1`
- **Root Cause Analysis:**  
  Initial script hardcoded `userId = 3`. Following database re-seeding, `student1` was assigned user ID 1, causing checkout to throw `ResourceNotFoundException: Borrower not found with id : '3'`.
- **Fix Applied:**  
  Updated script to dynamically query `/api/auth/me` with the authenticated student token and extract `$studentId = $me.data.id`.
- **Verification Method:**  
  Re-ran script and verified checkout resolved user ID dynamically across any database state.
- **Status:** **RESOLVED**

---

### BUG-07: Schema Constraint Violation on Multi-Unit Serialized Asset Checkout
- **Defect Title:** Multi-unit checkout failed on serialized asset with MySQL constraint `chk_br_individual_status`.
- **Severity:** Medium
- **Category:** Database Constraints & Business Logic
- **Affected Component:** `qa/integration/test-all-flows.ps1`
- **Root Cause Analysis:**  
  The schema defines `chk_br_individual_status` enforcing `(resource_unit_id IS NULL) OR (quantity_issued = 1 AND status NOT IN ('PARTIALLY_RETURNED','CLOSED'))`. Testing multi-unit checkout and partial return on Resource 5 (`Tektronix Oscilloscope`, tracked in `INDIVIDUAL` mode with barcoded units) violated the database constraint.
- **Fix Applied:**  
  Targeted Resource 6 (`Raspberry Pi 4`, tracked in `QUANTITY` bulk inventory mode without individual serial barcodes) for multi-unit reservation, checkout, and partial return testing.
- **Verification Method:**  
  Executed multi-unit checkout (qty 2), partial return (qty 1 -> `PARTIALLY_RETURNED`), and full return (qty 1 -> `RETURNED`) with 100% database constraint compliance.
- **Status:** **RESOLVED**

---

### BUG-08: Jackson Serialization Crash on Java 8 Date in CirculationRetryOrchestrator
- **Defect Title:** `CirculationRetryOrchestrator` crashed with `InvalidDefinitionException` when serializing `LocalDate dueDate` for Level-0 idempotency storage.
- **Severity:** High
- **Category:** Application Core / Resilience
- **Affected Component:** `CirculationRetryOrchestrator.java`
- **Root Cause Analysis:**  
  `CirculationRetryOrchestrator` instantiated a raw `private final ObjectMapper objectMapper = new ObjectMapper();`. By default, vanilla Jackson does not include `JavaTimeModule` for JSR-310 types (`java.time.LocalDate`). When `objectMapper.writeValueAsString(dto)` was invoked on a `CheckoutRequestDto` containing `dueDate`, Jackson threw:
  `com.fasterxml.jackson.databind.exc.InvalidDefinitionException: Java 8 date/time type java.time.LocalDate not supported by default: add Module "com.fasterxml.jackson.datatype:jackson-datatype-jsr310"`.
- **Fix Applied:**  
  Injected Spring Boot's pre-configured `ObjectMapper` bean (which registers `JavaTimeModule` and JSR-310 date formatting automatically) via constructor injection into `CirculationRetryOrchestrator`. Recompiled fat JAR and created dedicated regression unit test `testBug08LocalDateSerialization()` in `CirculationArchitectureTests.java`.
- **Verification Method:**  
  1. Unit test passed in `mvn test`:
     `BUG-08: CirculationRetryOrchestrator correctly serializes LocalDate in CheckoutRequestDto with Spring ObjectMapper`
  2. End-to-end checkout with idempotency key succeeded with HTTP 200 and valid JSON response.
- **Status:** **RESOLVED**

---

### BUG-09: Axe-Core WCAG 2 AA Color Contrast Violations on Badges and Navigation Headers
- **Defect Title:** Status badges, table column headers, and sidebar navigation headers fell below WCAG 2 AA 4.5:1 minimum color contrast ratio.
- **Severity:** Low / Aesthetic
- **Category:** Accessibility (WCAG 2.1 AA)
- **Affected Components:**
  - `borrowhub-frontend/src/components/common/Badge.jsx`
  - `borrowhub-frontend/src/pages/catalog/ResourceCatalogPage.jsx`
  - `borrowhub-frontend/src/pages/borrowing/MyBorrowingsPage.jsx`
  - `borrowhub-frontend/src/components/layout/Sidebar.jsx`
- **Root Cause Analysis:**  
  - In `Badge.jsx`, variant text colors used 700-shades on 50-tints (e.g., `text-emerald-700` on `bg-emerald-50`, `text-amber-700` on `bg-amber-50`), yielding ratios between 3.8:1 and 4.4:1 (below the 4.5:1 threshold).
  - In `MyBorrowingsPage.jsx`, table headers and metadata used `text-slate-400` on `bg-slate-50` and white backgrounds, yielding low contrast (~2.5:1).
  - In `Sidebar.jsx`, section category labels used `text-slate-400` on white, failing WCAG AA.
- **Fix Applied:**  
  1. Updated `Badge.jsx` to high-contrast WCAG AAA palettes (`text-blue-900`, `text-emerald-900`, `text-amber-950`, `text-rose-900`, `text-indigo-900`, `text-slate-800`), achieving contrast ratios between 9.2:1 and 14.2:1.
  2. Updated `ResourceCatalogPage.jsx` category pills and subtexts to `text-slate-700` and `text-blue-700`.
  3. Updated `MyBorrowingsPage.jsx` `thead` to `text-slate-700` (9.5:1 ratio) and subtext labels to `text-slate-600`.
  4. Updated `Sidebar.jsx` section headers to `text-slate-700` and `text-indigo-700`.
- **Verification Method:**  
  Executed `@axe-core/playwright` automated test suite across all three pages:
  - `/login`: 0 violations
  - `/catalog`: 0 violations (0 critical, 0 serious, 0 moderate, 0 minor)
  - `/borrowings`: 0 violations (0 critical, 0 serious, 0 moderate, 0 minor)
- **Status:** **RESOLVED**
