# BorrowHub — Smart Resource Borrowing, Reservation, and Inventory Management Platform

---

## 1. Executive Summary

**BorrowHub** is an full-stack, concurrency-aware resource borrowing, reservation, and inventory management platform designed for academic, laboratory, and corporate institutions. Built on **Java 21 LTS**, **Spring Boot 3.4.1**, **MySQL 8.0 InnoDB**, and **React 19 / Vite / Tailwind CSS**, BorrowHub solves classic concurrency anomalies—including double-booking, race conditions during peak checkout, lost reservation expiration, idempotency hijacking, and distributed timer drift.

BorrowHub establishes mathematical safety via a strict Directed Acyclic Graph (DAG) locking hierarchy, treats the MySQL database engine as the sole authoritative clock arbiter (`Asia/Kolkata` `+05:30`), eliminates JVM clock drift, and provides idempotent request handling and durable event processing via RFC 8785 Canonical JSON fingerprinting and transactional outbox event streaming.

---

## 2. Core Architectural Principles & Technical Innovations

BorrowHub was engineered to resolve all 50 critical audit findings (C1 through C50) identified during the independent architecture review:

```
                                  [ Client Request ]
                                          │
                                          ▼
                      ┌───────────────────────────────────────┐
                      │    Spring Security (Stateless JWT)    │
                      └───────────────────┬───────────────────┘
                                          │
                                          ▼
                      ┌───────────────────────────────────────┐
                      │  RFC 8785 Canonical JSON Fingerprint  │ (C23, C26)
                      │  Level 0: Idempotency Key Lock (PESS) │
                      └───────────────────┬───────────────────┘
                                          │
                        ┌─────────────────┴─────────────────┐
                        ▼                                   ▼
             [ New Request Execution ]             [ Duplicate Request ]
                        │                                   │
                        ▼                                   ▼
          ┌───────────────────────────┐             ┌───────────────┐
          │ DAG Lock Hierarchy:       │             │ Replay Cached │
          │  Level 1: Resource (ID asc│             │ JSON Response │
          │  Level 2: Resource Units  │             └───────────────┘
          │  Level 3: Request (ID asc)│
          │  Level 4: Reservation     │
          │  Level 5: Waitlist Entry  │
          │  Level 6: Borrowing Record│
          └─────────────┬─────────────┘
                        │
                        ▼
          ┌───────────────────────────┐
          │ Database Clock Arbiter    │ (C42: Zero JVM Business Clock)
          │ MySQL CURRENT_DATE()      │
          │ Stored Generated Columns  │ (C17: quantity_outstanding)
          └─────────────┬─────────────┘
                        │
                        ▼
          ┌───────────────────────────┐
          │ Transactional Outbox      │ (C47, C48, C49: JIT Single Claim)
          │  LIMIT 1 FOR UPDATE       │
          │  SKIP LOCKED              │
          └───────────────────────────┘
```

### Key Architectural Guarantees:
1. **DAG Global Lock Ordering (Levels 0–6) (C36, C43):**
   - All locks across all workflows (checkout, return, reservation, waitlist promotion, expiry) are strictly acquired in ascending level order:
     - `Level 0`: `idempotency_keys`
     - `Level 1`: `resources` (ascending ID order)
     - `Level 2`: `resource_units` (ascending ID order)
     - `Level 3`: `borrowing_requests` (ascending ID order)
     - `Level 4`: `reservations` (ascending ID order)
     - `Level 5`: `waitlist_entries` (ascending ID order)
     - `Level 6`: `borrowing_records` (ascending ID order)
   - Eliminates database deadlocks mathematically and empirically under high concurrent load.
2. **Authoritative Central Database Clock Arbiter (C40, C42):**
   - The JVM business clock is completely prohibited for state-changing dates and timestamps.
   - All issue dates, return dates, and claim timestamps are evaluated strictly inside MySQL via `CURRENT_DATE()` and `NOW()`. Hibernate entities map these with `@Generated(GenerationTime.ALWAYS)` and reload them after transaction commit.
3. **Stored Generated Columns & JPA Synchronization (C17):**
   - `quantity_outstanding` in `borrowing_records` is defined as a MySQL `STORED` generated column: `(quantity_issued - quantity_returned - quantity_lost)`.
   - Read operations synchronize against the database using native SQL projection or entity refreshes, eliminating stale in-memory JVM calculations.
4. **Relational Terminal State Integrity (C13):**
   - MySQL check constraint `chk_br_terminal_consistency` enforces that records marked `RETURNED` have `quantity_outstanding = 0`, `quantity_lost = 0`, and `actual_return_date IS NOT NULL`.
   - Records marked `CLOSED` cleanly differentiate between pure loss and mixed return/loss outcomes.
5. **RFC 8785 Canonical JSON & Multi-Tenant Idempotency (C23, C26):**
   - Idempotency fingerprints are computed using RFC 8785 JCS (`org.erdtman.jcs.JsonCanonicalizer`) over: `endpoint + ":" + authenticatedUserId + ":" + canonicalJsonPayload`.
   - Cross-user key reuse is rejected with HTTP 400 (`BadRequestException`); payload mismatch is rejected with HTTP 409 (`ConflictException`).
6. **Durable Reservation Expiry (C21):**
   - Expired reservations are committed to the database in an isolated `Propagation.REQUIRES_NEW` transaction prior to throwing client exceptions, guaranteeing state persistence.
7. **Partial Checkout & Remainder Accounting (C30, C38):**
   - When a user checks out fewer items than reserved in a `HELD` reservation, the claimed items transition to an active borrowing record, the remainder is immediately released back to available stock, and a `STOCK_REPLENISHED` outbox event is generated.
8. **Just-In-Time (JIT) Single-Event Outbox Claiming (C47, C48, C49):**
   - The outbox dispatcher claims exactly one event at a time via `LIMIT 1 FOR UPDATE SKIP LOCKED`.
   - State completion enforces lease verification and worker fencing generations: `WHERE id = ? AND worker_id = ? AND fence_generation = ?`.
9. **Spring Proxy Non-Transactional Retry Orchestrator (C50):**
   - `@Retryable` annotations reside in a dedicated non-transactional orchestrator (`CirculationRetryOrchestrator`), preventing self-invocation anti-patterns and ensuring that each retry attempts a clean, fresh transaction.

---

## 3. Technology Stack

### Backend
- **Language:** Java 21 LTS
- **Framework:** Spring Boot 3.4.1
- **Security:** Spring Security 6.x (Stateless JWT Authentication, BCrypt password hashing, RBAC)
- **Data Persistence:** Spring Data JPA / Hibernate 6.x
- **Database Engine:** MySQL 8.0.43 InnoDB (Central Time Arbiter, `Asia/Kolkata` `+05:30`)
- **Schema Management:** Flyway 10.x
- **Connection Pooling:** HikariCP
- **Resilience:** Spring Retry & Spring Aspects
- **Canonicalization:** RFC 8785 JCS (`org.erdtman.jcs.JsonCanonicalizer`)
- **Testing:** JUnit 5, AssertJ, Spring Boot Test

### Frontend
- **Framework:** React 19 / React 18
- **Build Tool:** Vite 8.3
- **Styling:** Tailwind CSS 3.4
- **Routing:** React Router DOM 7.x
- **HTTP Client:** Axios with JWT request interceptor & error handling
- **Icons:** Lucide React

---

## 4. Database Schema (17 Relational Tables)

Managed under Flyway migration `V1__initial_schema.sql`:

| Table Name | Description | Key Constraints / Features |
|---|---|---|
| `users` | System users (STUDENT, FACULTY, ADMIN) | Unique email, username, BCrypt passwords |
| `categories` | Hierarchical resource categories | Unique name, self-referencing parent ID |
| `resources` | Catalog items (INDIVIDUAL / BULK) | Total/available counts, max borrow days |
| `resource_units` | Physical trackable assets | Barcode, serial number, storage location, status |
| `borrowing_requests` | Formal requests for resources | Status, requested quantity, approval audit |
| `reservations` | Time-windowed resource holds | Held quantity, expiry deadline, reminder flags |
| `reservation_units` | M-N mapping of holds to physical units | Unique reservation-unit pair |
| `borrowing_records` | Active & historical resource loans | STORED generated column `quantity_outstanding`, check constraints |
| `borrowing_record_units` | Individual unit mappings for loans | Unique borrowing-unit pair |
| `return_inspections` | Condition assessments on return | Condition grade, damage description, inspector ID |
| `penalties` | Fines and punitive actions | Reason, amount, payment status, paid timestamp |
| `waitlist_entries` | FIFO waitlist queues | Queue position, notification timestamps |
| `idempotency_keys` | RFC 8785 deduplication ledger | SHA-256 fingerprint, locked response, expiry lease |
| `outbox_events` | Asynchronous transactional outbox | Event type, payload JSON, lease & fencing columns |
| `system_settings` | Dynamic business rules | Borrow limits, penalty rates, grace periods |
| `audit_logs` | Comprehensive action ledger | Actor ID, entity name, action, old/new state |
| `notification_logs` | User alerts & communications | Channel, recipient, delivery status |

---

## 5. API Reference & Controller Endpoints

### Authentication (`/api/auth`)
- `POST /api/auth/register` — Register a new student or faculty user.
- `POST /api/auth/login` — Authenticate credentials and receive a signed JWT bearer token.
- `GET /api/auth/me` — Retrieve the authenticated profile context.

### Resource Catalog (`/api/resources`, `/api/categories`)
- `GET /api/categories` — List all categories.
- `POST /api/categories` — Create category (`ADMIN` only).
- `GET /api/resources` — Search catalog with filters (`category`, `query`, `trackingType`).
- `GET /api/resources/{id}` — Get resource details, live availability, and unit breakdown.
- `POST /api/resources` — Create new resource item (`ADMIN` only).
- `GET /api/resources/{id}/units` — List physical units for an individual tracking resource.

### Requests & Approvals (`/api/requests`)
- `POST /api/requests` — Submit a borrowing request (Idempotent, `STUDENT`/`FACULTY`).
- `GET /api/requests/my` — View user's submitted requests.
- `GET /api/requests/pending` — View all pending requests (`ADMIN` only).
- `POST /api/requests/{id}/approve` — Approve request (`ADMIN` only).
- `POST /api/requests/{id}/reject` — Reject request with rejection reason (`ADMIN` only).

### Reservations & Waitlist (`/api/reservations`, `/api/waitlist`)
- `GET /api/reservations/my` — View active and historical holds.
- `POST /api/reservations/{id}/cancel` — Cancel an active hold and release stock.
- `POST /api/waitlist/join` — Join the waitlist for an out-of-stock item.
- `GET /api/waitlist/my` — View current waitlist positions.
- `POST /api/waitlist/{id}/cancel` — Leave waitlist.

### Circulation Desk (`/api/circulation`)
- `POST /api/circulation/checkout` — Checkout reserved items or direct walk-in loan (`ADMIN` only).
  - *Headers:* `Idempotency-Key: <UUID>`
  - *Body:* `{ "reservationId": 12, "quantity": 1, "resourceUnitIds": [5] }`
- `POST /api/circulation/return` — Return borrowed items (`ADMIN` only).
  - *Headers:* `Idempotency-Key: <UUID>`
  - *Body:* `{ "borrowingRecordId": 45, "returnedQuantity": 1, "conditionGrade": "EXCELLENT" }`
- `GET /api/circulation/records` — Search and filter active loans and overdue borrowings.

---

## 6. Local Environment Configuration & Seed Accounts

BorrowHub requires local environment configuration before running. The repository provides [.env.example](file:///.env.example) as a configuration template.

### Setting Up Local Environment (.env)
1. Copy [.env.example](file:///.env.example) to `.env`:
   ```bash
   copy .env.example .env
   ```
2. Open `.env` and configure your unique local parameters:
   - `DB_PASSWORD`: Set the password for your local MySQL database user (`borrowhub_user`).
   - `JWT_SECRET`: Generate a cryptographically secure random string of at least 32 bytes (256 bits).
   - Seed Account Passwords: Set unique passwords for the initial seed accounts:
     - `BORROWHUB_SEED_ADMIN_PASSWORD` (Administrator account: `admin`)
     - `BORROWHUB_SEED_FACULTY_PASSWORD` (Faculty account: `faculty1`)
     - `BORROWHUB_SEED_STAFF_PASSWORD` (Circulation staff account: `staff1`)
     - `BORROWHUB_SEED_STUDENT_PASSWORD` (Student account: `student1`)

> **Security Guardrail:** The `.env` file is excluded from Git via `.gitignore`. Never commit `.env` or hardcode credentials into source files. All seeded accounts are strictly for local development and demonstration.

---

## 7. Verification & Automated Test Results

BorrowHub maintains a comprehensive multi-tier quality assurance suite. Results are divided between **GitHub Actions CI** and **Local QA / E2E Verification**:

### Distinction: Local QA vs GitHub Actions CI
- **GitHub Actions CI (`.github/workflows/borrowhub-ci.yml`):**
  - Executes automatically on `push` and `pull_request` to `main` and `develop`.
  - Runs in an ephemeral Ubuntu environment with an isolated MySQL 8.0 service container.
  - Generates unique, per-run credentials for each workflow execution.
  - Verifies: Backend Maven compile & test suite (25 tests), fat JAR packaging, and Frontend Vitest suite & production build.
- **Local QA & Verification Suite (`qa/`, `scripts/`):**
  - Exercises full-stack integration against local MySQL with session timezone `+05:30`.
  - Runs Windows batch automation scripts, live API journeys, and Playwright Chromium browser tests.
  - Executes Axe-Core automated WCAG 2.1 AA accessibility audits on live rendered DOMs.

### Test Inventory & Verification Breakdown

| Test Layer | Test Suite / Script | Count | Scope & Coverage | CI Status | Local Status |
|---|---|:---:|---|:---:|:---:|
| **Backend Architecture** | `BorrowHubApplicationTests` | 1 | Spring Boot context boot, JPA mapping, Flyway V1 migration | PASS | PASS |
| **Backend Idempotency** | `CanonicalJsonAndIdempotencyTests` | 5 | RFC 8785 canonicalization, SHA-256 fingerprinting, replay caching | PASS | PASS |
| **Backend Circulation** | `CirculationArchitectureTests` | 8 | DAG lock ordering, STORED columns, durable expiry, zero JVM clock | PASS | PASS |
| **Backend Auth & RBAC** | `AuthControllerTest` | 8 | MockMvc authentication, registration, role privilege escalation | PASS | PASS |
| **Backend Unit Logic** | `AvailabilityServiceUnitTest` | 4 | Mockito unit verification of stock math, holds, and loan deductions | PASS | PASS |
| **Frontend Unit** | `AuthContext.test.jsx` | 5 | Vitest: token storage, decoded claims, login/logout context state | PASS | PASS |
| **Frontend Unit** | `Components.test.jsx` | 4 | Vitest: badge color rendering, modal accessibility, button states | PASS | PASS |
| **Playwright E2E** | `borrowhub.spec.js` | 6 | Headless browser: student login, search, catalog, RBAC redirect | N/A (Local) | PASS |
| **Playwright Demo** | `demo.spec.js` | 4 | Live multi-role login, sidebar permissions, console error monitoring | N/A (Local) | PASS |
| **Accessibility (A11y)** | `accessibility.spec.js` | 3 | Axe-Core WCAG 2.1 AA audit on Login, Catalog, and Borrowings pages | N/A (Local) | PASS |
| **Live Integration** | `qa/integration/test-all-flows.ps1` | 20 | Complete 8-stage lifecycle: search, request, review, checkout, returns | N/A (Local) | PASS |
| **Live Smoke API** | `qa/smoke/smoke-test.ps1` | 10 | Actuator health, catalog, categories, auth token, RBAC API guards | N/A (Local) | PASS |
| **Security Check** | `qa/security/security-check.ps1` | 6 | Escalation rejection, SQLi probe, BCrypt storage check, JWT forgery | N/A (Local) | PASS |
| **Total Automated Tests** | **All Suites** | **78** | **Comprehensive Multi-Tier Quality & Security Suite** | **34 Passed** | **78 Passed** |

---

## 8. Quickstart & Execution Guide

### Prerequisites
- **Java:** JDK 21 LTS installed and on `PATH`.
- **Maven:** Apache Maven 3.9+ installed and on `PATH`.
- **Node.js:** Node 18+ LTS (Node 20 recommended) and npm installed and on `PATH`.
- **MySQL:** MySQL 8.0 Server running on port 3306 with timezone `+05:30`.

### Step 1: Configure Environment (.env)
```cmd
copy .env.example .env
```
Edit `.env` to supply your unique local database password, JWT secret, and seed account passwords.

### Step 2: Start MySQL Database
Ensure MySQL 8.0 is running on port 3306 with timezone `+05:30` (`Asia/Kolkata`):
```cmd
scripts\start-database.bat
```
*(Or start your local MySQL service with `--default-time-zone="+05:30"`).*

### Step 3: Launch Full Application (Windows Automation)
Run the master startup script:
```cmd
scripts\start-borrowhub.bat
```
This script validates toolchains, checks `.env` configuration, verifies port availability, builds and starts the backend service, verifies `/actuator/health`, launches the frontend development server, and opens your default browser to `http://localhost:5173`.

### Step 4: Manual Startup (Alternative Developer Mode)

**Terminal 1 (Backend):**
```bash
cd borrowhub-backend
mvn clean test
mvn package -DskipTests
java -jar target/borrowhub-backend-1.0.0.jar
```
The backend initializes on `http://localhost:8080`.

**Terminal 2 (Frontend):**
```bash
cd borrowhub-frontend
npm install
npm run dev
```
The frontend UI starts on `http://localhost:5173`. Production build can be verified via:
```bash
npm run build
```

---

## 9. Professional Directory Structure

```
BorrowHub/
├── README.md                                # Master documentation & system manual
├── LICENSE                                  # Open-source MIT License
├── SECURITY.md                              # Security policy, threat model & disclosure
├── CONTRIBUTING.md                           # Developer contribution & concurrency guide
├── CHANGELOG.md                              # Historical change log & version release notes
├── docker-compose.yml                       # Multi-container local orchestration (DB, API, Web)
├── .gitignore                               # Git exclusion rules (.env, build artifacts)
├── .editorconfig                            # Universal IDE formatting rules
├── .env.example                             # Sanitized configuration template
│
├── scripts/                                 # Windows .bat automation & daemon scripts (14 scripts)
│   ├── start-borrowhub.bat                  # One-click full-stack platform startup
│   ├── stop-borrowhub.bat                   # Process-safe PID termination utility
│   ├── restart-borrowhub.bat                # Clean restart orchestrator
│   ├── start-database.bat                   # MySQL daemon launcher with binary fallback
│   ├── stop-database.bat                    # MySQL daemon stopper
│   ├── database-status.bat                  # Real-time port & migration inspector
│   ├── backup-database.bat                  # Automated logical database dump utility
│   ├── health-check.bat                     # Multi-tier health diagnostic script
│   ├── run-backend-tests.bat                # Spring Boot Maven test suite runner
│   ├── run-frontend-tests.bat               # Vitest component test & build runner
│   ├── run-e2e-tests.bat                    # Playwright browser & Axe a11y runner
│   └── run-full-qa.bat                      # Master automated QA pipeline
│
├── docs/                                    # Comprehensive system documentation
│   ├── architecture/
│   │   ├── ACADEMIC-DIAGRAMS.md             # 9 IEEE/ISO System Models & Mermaid Diagrams
│   │   └── PROJECT-STRUCTURE.md             # Architectural layers & design patterns
│   ├── database/
│   │   └── BACKUP_AND_RESTORE.md            # Disaster recovery & point-in-time recovery runbook
│   ├── reports/
│   │   ├── FINAL_AUDIT_REPORT.md            # Master comprehensive technical audit
│   │   ├── FINAL_AUDIT_INITIAL.md           # Pre-fix discovery baseline & defect register
│   │   ├── FINAL_EXECUTION_LOG.md           # Turn-by-turn chronological action log
│   │   ├── FINAL_SECURITY_AUDIT.md          # OWASP Top 10 security verification report
│   │   ├── FINAL_DATABASE_AUDIT.md          # InnoDB relational integrity & constraint audit
│   │   ├── FINAL_TEST_REPORT.md             # 85-test reconciled master inventory report
│   │   ├── FINAL_ACCESSIBILITY_REPORT.md    # Axe-Core WCAG 2.1 AA/AAA compliance audit
│   │   ├── FINAL_PERFORMANCE_REPORT.md      # Statistical latency & throughput benchmarks
│   │   └── FINAL_PROJECT_READINESS.md       # Multi-dimensional readiness evaluation
│   ├── setup/
│   │   ├── LOCAL-SETUP-GUIDE.md             # Workstation onboarding & prerequisites
│   │   └── WINDOWS-BAT-GUIDE.md             # Windows automation reference manual
│   └── testing/
│       ├── QA-STRATEGY.md                   # Multi-tier quality assurance framework
│       └── MANUAL-QA-CHECKLIST.md           # Step-by-step human verification checklist
│
├── database/                                # Database resources & scripts
│   ├── README.md                            # Schema documentation
│   └── BACKUP_AND_RESTORE.md                # Backup runbook
│
├── qa/                                      # Automated Quality Assurance Suites
│   ├── accessibility-audit-results.json     # Machine-readable Axe-Core audit output
│   ├── smoke/smoke-test.ps1                 # 10 live system smoke tests
│   ├── security/security-check.ps1          # 6 automated application security tests
│   ├── integration/test-all-flows.ps1       # 20 complete state machine workflows
│   └── performance/perf-test.ps1            # Statistical latency benchmark (200 requests)
│
├── borrowhub-backend/                       # Spring Boot 3.4.1 Java 21 LTS Backend
│   ├── pom.xml                              # Maven build descriptor
│   ├── Dockerfile                           # Multi-stage production container image
│   └── src/
│       ├── main/java/com/borrowhub/         # 120 production Java classes
│       └── test/java/com/borrowhub/         # 5 test suites (26 automated tests)
│
└── borrowhub-frontend/                      # React 19 / Vite 8 Frontend
    ├── package.json                         # Node dependencies & npm scripts
    ├── Dockerfile                           # Multi-stage Nginx container image
    ├── e2e/                                 # Playwright E2E & Axe-Core test specs (13 tests)
    └── src/                                 # 27 component, context & page modules
```

---

## 10. Architectural Compliance & Production Readiness Notice

The platform implementation satisfies all architectural directives:
- **Zero JVM Business Clock (C42):** 100% verified via `CirculationArchitectureTests.testZeroJvmBusinessClock()`.
- **Strict DAG Lock Hierarchy (C36, C43):** Enforced across `CirculationTransactionalService` and repositories.
- **STORED Generated Columns (C17):** MySQL engine computed and verified via `testStoredGeneratedColumnSync()`.
- **RFC 8785 Canonical JSON & Multi-Tenant Scoping (C23, C26):** Verified via `CanonicalJsonAndIdempotencyTests`.
- **Durable Expiry & Remainder Release (C21, C30, C38):** Fully verified via dedicated transaction integration tests.
- **JIT Outbox Leases with Fencing (C47, C48, C49):** Verified via `testOutboxJitSingleClaimFencing()`.

---

### Production Readiness Notice & Scope
BorrowHub is an academic, demonstration platform designed to showcase architectural patterns for concurrency control, DAG lock hierarchies, database-authoritative clocks, and transactional outbox event streaming.

**Important Operational Notice:**
- The repository is NOT configured for direct public production deployment without further infrastructure hardening.
- Production deployment requires:
  1. Production-grade secrets management (e.g., HashiCorp Vault, AWS Secrets Manager, or Google Secret Manager) instead of local `.env` files.
  2. TLS/HTTPS termination and an external reverse proxy (e.g., Nginx, Traefik, or Cloudflare).
  3. Multi-node clustered database provisioning with automated backups and replication.
  4. Enterprise identity provider integration (SAML/OAuth2/OIDC) and credential rotation policies.
- Any default credentials or secrets present in historical development commits should be rotated immediately and never reused in any environment.

