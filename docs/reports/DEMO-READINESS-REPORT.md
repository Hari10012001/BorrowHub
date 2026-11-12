# BorrowHub (APPJFS12) — Final Live Demo Readiness & Acceptance Report

**Execution Date & Time:** 2026-11-12 17:37:00 IST (UTC+05:30)  
**Lead Auditor / Architect:** Principal Java Architect, Spring Transaction & MySQL Specialist  
**Git Baseline Commit:** `b4fe57f` (`fix(a11y): resolve WCAG contrast violations, reconcile test inventory, and close final audit defects`)  
**Git Working Tree Status:** Clean  
**Final Verdict:** **DEMO READY**

---

## 1. Startup & Service Health Verification

The platform was initialized and validated using the official Windows batch automation and verified process checks.

| Service Tier | Port | Technology | Process Verification | Health Endpoint Status | Result |
|---|:---:|---|---|:---:|:---:|
| **Database** | 3306 | MySQL 8.0.43 InnoDB | Dedicated `borrowhub_mysql_data` instance | Port 3306 LISTENING, Timezone `+05:30` | **HEALTHY** |
| **Backend API** | 8080 | Spring Boot 3.4.1 / Java 21 | Executable Fat JAR (`runtime/borrowhub-backend.jar`) | `http://localhost:8080/actuator/health` -> `{"status":"UP"}` | **HEALTHY** |
| **Frontend UI** | 5173 | React 19 / Vite 6 | Vite Client Dev Server with HMR | `http://localhost:5173` -> HTTP 200 OK | **HEALTHY** |

### Automation Script Safety Check:
- Verified `scripts/stop-borrowhub.bat` process termination safety: Checked that termination uses PowerShell CIM/WMI `CommandLine` queries (`borrowhub-frontend|vite` and `borrowhub-backend|BorrowHubApplication`), releasing ports 8080 and 5173 without terminating unrelated developer workloads or host MySQL services.
- Verified `scripts/start-borrowhub.bat` startup gating: Browser launch is strictly gated on verified HTTP 200 frontend response.

---

## 2. Role-Based Authentication & Navigation Verification (Browser Test)

Live browser verification was executed on Headless Chromium via Playwright (`borrowhub-frontend/e2e/demo.spec.js`):

| Role Tested | Authentication Result | Permitted Navigation Elements Observed | Restricted Routes Confirmed Blocked | Result |
|---|:---:|---|---|:---:|
| **Student** | **PASS** | `Browse Catalog`, `My Borrowings`, `My Reservations`, `My Waitlist` | `Circulation Desk`, `Pending Approvals`, `Administration` (Redirected to `/catalog`) | **VERIFIED** |
| **Faculty** | **PASS** | `Browse Catalog`, `My Borrowings`, `Circulation Desk` (`Issue & Returns`), `Pending Approvals` | `Administration -> Inventory & Assets` (Redirected to `/catalog`) | **VERIFIED** |
| **Administrator** | **PASS** | Full Access: `Browse Catalog`, `Circulation Desk`, `Pending Approvals`, `Administration -> Inventory & Assets` | All routes accessible | **VERIFIED** |

*Note: User credentials conform to standard security practices and are not exposed in plaintext within this report.*

---

## 3. End-to-End Workflow Demonstration

All 8 requested business and lifecycle workflows were executed live against the running application and MySQL 8.0 database (`qa/integration/test-all-flows.ps1`):

| # | Demonstrated Workflow | Test Action & Endpoints | Empirical Outcome | State Transition / Status |
|---|---|---|---|:---:|
| **1** | **Resource Search** | `GET /api/resources?search=Arduino` | Found exact item "Arduino Mega 2560 Starter Kit" | **PASS** |
| **2** | **Reservation Lifecycle** | `POST /api/reservations` & `POST /api/reservations/{id}/cancel` | Created hold (stock decremented), explicitly cancelled (stock restored) | `HELD` -> `CANCELLED` (**PASS**) |
| **3** | **Borrowing Request** | `POST /api/borrowings/requests` | Student submitted IoT request for Raspberry Pi (Resource 6) | `PENDING_REVIEW` (**PASS**) |
| **4** | **Faculty Approval** | `POST /api/borrowings/requests/{id}/review` | Faculty reviewed and approved request | `APPROVED` (**PASS**) |
| **5** | **Admin Checkout** | `POST /api/circulation/checkout` | Checked out 2 units with Level-0 `Idempotency-Key` + replay verified | `ACTIVE` (**PASS**) |
| **6** | **Partial Return** | `POST /api/circulation/return` | Returned 1 of 2 units; remaining 1 unit held | `PARTIALLY_RETURNED` (**PASS**) |
| **7** | **Full Return** | `POST /api/circulation/return` | Returned remaining 1 unit; 0 units outstanding | `RETURNED` (**PASS**) |
| **8** | **Waitlist Lifecycle** | `POST /api/waitlist` & `POST /api/waitlist/{id}/cancel` | Queued student for Dell laptop; cancelled queue cleanly | `WAITING` -> `CANCELLED` (**PASS**) |

---

## 4. Database Inventory Consistency

Inventory tracking was verified across the destructive testing lifecycle on Resource 6 (`Raspberry Pi 4 Model B`, `QUANTITY` mode):
- **Pre-Test Baseline Available Quantity:** `10` units
- **During Hold Reservation (Qty: 2):** Decremented to `8` units
- **During Active Loan (Qty: 2):** Maintained at `8` units
- **After Partial Return (Qty: 1 returned):** Incremented to `9` units
- **After Full Return (Qty: 1 returned):** Restored to `10` units
- **Post-Test Final Available Quantity:** `10` units
- **Mathematical Consistency Verification:** `Initial (10) == Final (10)` (**100% INVENTORY CONSISTENCY CONFIRMED**).

---

## 5. Application Stability & Log Inspection

### 5.1 Browser Console & Page Errors
- **Audited Pages:** `/login`, `/catalog`, `/borrowings`, `/reservations`, `/waitlist`, `/circulation`, `/approvals`, `/admin/resources`
- **Playwright Console Listener Results:** **0 console errors**, **0 uncaught page exceptions**.
- **Axe-Core Automated WCAG Audit:** **0 critical, 0 serious, 0 moderate, 0 minor accessibility violations** ([qa/accessibility-audit-results.json](file:///d:/HARIHARAN%20P/000_JAVA FULL STACK - Final Year Main_Projects - 2026_2027/012_BorrowHub - A Smart Resource Borrowing, Reservation and Inventory Management Platform/qa/accessibility-audit-results.json)).

### 5.2 Backend Application Logs
- **Log Source:** Spring Boot runtime log (`logs/backend.log` and active JVM daemon log)
- **Log Inspection Findings:**
  - Zero `NullPointerException`, zero `InvalidDefinitionException`, zero unhandled HTTP 500 errors.
  - Level-0 Idempotency cache replayed successfully: `Replaying cached checkout response for key: 1828c773-51ec-402b-be0c-50eaf1df41af`.
  - Transactional Outbox replenishment dispatched without deadlock: `Handling STOCK_REPLENISHED for resourceId: 6`.

---

## 6. Documented Operational Limitations

1. **Local Workstation Scope:** All verifications were executed on a local single-instance workstation environment (Windows 10/11, JDK 21, MySQL 8.0 local datadir). Multi-node distributed clustering, cloud load balancers, and container orchestration (Kubernetes) are outside the scope of this standalone desktop demonstration.
2. **Remote CI/CD Pipeline:** `.github/workflows/borrowhub-ci.yml` syntax is validated and ready for remote execution, but marked as `NOT RUN` in the repository due to the absence of a configured remote GitHub origin.

---

## 7. Final Demo Readiness Verdict

# **FINAL VERDICT: DEMO READY**

The BorrowHub (APPJFS12) platform meets all functional, architectural, security, accessibility, and automation criteria. It starts cleanly, enforces strict role-based separation, manages inventory with mathematical consistency, operates with zero console/backend errors, and is ready for live presentation and user demonstration.
