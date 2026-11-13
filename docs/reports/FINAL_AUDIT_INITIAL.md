# BorrowHub – Initial Discovery and Pre-Fix Audit Baseline

**Project Code:** APPJFS12  
**Audit Baseline Timestamp:** 2026-11-13T13:10:00+05:30  
**Baseline Git Commit:** `aa3b6cec3ad2769d68885992051d23f11138807a`  
**Auditor:** Senior Full Stack Architect & Application Security Engineer  

---

## 1. Executive Summary

BorrowHub is a resource borrowing, reservation, and inventory management platform built with Spring Boot 3.4 (Java 21) and React 19 (Vite 8) backed by MySQL 8.0 InnoDB.

Prior to implementing enhancements, an exhaustive inspection of the codebase, database schema, Windows automation scripts, security guards, accessibility audits, and test suites was conducted.

The baseline test run confirmed that the core architecture, 26 backend unit/integration tests, 9 Vitest frontend tests, and the Vite production build pass cleanly. However, five concrete issues (spanning script automation safety, authorization boundaries/IDOR, database backup tools, and Spring Data serialization warnings) were identified.

---

## 2. Issues Discovered and Categorized by Severity

| ID | Category | Severity | File / Component | Description |
|---|---|---|---|---|
| **ISSUE-01** | Automation | **HIGH** | `scripts/start-database.bat` | `sc query MySQL80` succeeds for stopped service, but `net start MySQL80` fails with Access Denied in non-admin shells. Parsing `%ERRORLEVEL%` inside parenthesized blocks evaluated to `0`, causing false success and skipping fallback to local `mysqld.exe`. |
| **ISSUE-02** | Security | **MEDIUM** | `com.borrowhub.controller.BorrowingController` | IDOR vulnerability: `GET /api/borrowings/{id}` returned borrowing records without verifying whether `currentUser` owns the record or holds `ADMIN`/`FACULTY`/`STAFF` authority. |
| **ISSUE-03** | Security | **MEDIUM** | `com.borrowhub.controller.ReservationController` | IDOR vulnerability: `GET /api/reservations/{id}` returned reservation records without checking user ownership or administrative authority. |
| **ISSUE-04** | Code Quality | **LOW** | `borrowhub-backend` Spring Data Web | Spring Boot logs warning on `PageImpl` JSON serialization stability; recommendation is enabling `pageSerializationMode = VIA_DTO`. |
| **ISSUE-05** | DevOps / DB | **LOW** | `database/` / `scripts/` | Missing standalone database backup and restore script/guide for operational disaster recovery. |
| **ISSUE-06** | Security | **INFORMATIONAL** | Git History (`pre-1dd7de3`) | Historical commits prior to credential decoupling contained development seed passwords. Active tree is clean (`.env` git-ignored), but rotation guidelines must be highlighted in `SECURITY.md`. |

---

## 3. Technology Stack Inventory

### Backend
- **Framework:** Spring Boot 3.4.1 (Java 21 LTS)
- **Security:** Spring Security 6.x (Stateless JWT HMAC-SHA256, BCrypt cost 12)
- **Persistence:** Spring Data JPA / Hibernate 6.x
- **Database Engine:** MySQL 8.0.43 InnoDB (ACID, Foreign Keys, 15 Check Constraints)
- **Migrations:** Flyway 10.x (`V1__initial_schema.sql` - 17 application tables + history)
- **Idempotency:** RFC 8785 canonical JSON digest (`SHA-256`) with atomic lease fencing
- **Scheduler:** Outbox single-claim JIT leases (C47), midnight overdue sweep (C40/C42), and reservation expiry (C21)

### Frontend
- **Framework:** React 19.x with Vite 8.x
- **Language:** JavaScript (ESNext)
- **Styling:** Tailwind CSS with high-contrast semantic palettes
- **State/Routing:** React Router v7, React Context (`AuthContext`)
- **HTTP Client:** Axios with bearer token interceptors and 401 redirect handling

### Testing & Quality Assurance
- **Backend Tests:** 26 automated tests (JUnit 5, Mockito, Spring Boot Test)
- **Frontend Tests:** 9 automated unit/component tests (Vitest, React Testing Library)
- **E2E Tests:** 13 Playwright browser tests (Functional workflows, role matrix, Axe-Core a11y)
- **Smoke Tests:** 10 PowerShell smoke checks
- **Security Scanner:** 6 automated security vulnerability checks
- **Integration Flows:** 20 complete state machine workflows
- **Performance Benchmark:** 50-request concurrent throughput stress test

---

## 4. Baseline Test Execution Results

```text
[Backend Maven Tests]
Tests run: 26, Failures: 0, Errors: 0, Skipped: 0
Build Status: SUCCESS (Total time: 42.045 s)

[Frontend Vitest Tests]
Test Files: 2 passed (2)
Tests:      9 passed (9)
Build Status: Vite build succeeded (dist/ assets generated cleanly)

[Database Connection]
MySQL Server 8.0.43 connected on 127.0.0.1:3306
Total Tables: 18 (17 tables + flyway_schema_history)
Migration Status: V1__initial_schema.sql applied successfully
Timezone: +05:30 (Asia/Kolkata)
```

---

## 5. Remediation Action Plan

1. **Fix `scripts/start-database.bat`**:
   Replace static `%ERRORLEVEL%` with dynamic `!ERRORLEVEL!` and verify listening port status before branching, ensuring seamless fallback to user-space `mysqld.exe`.
2. **Harden `BorrowingController` & `ReservationController`**:
   Add user ownership and role authorization checks on single-record retrieval endpoints (`/{id}`) to eliminate IDOR.
3. **Configure Spring Data Web Paging**:
   Add `@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)` in backend configuration to resolve serialization warnings.
4. **Create Database Backup & Recovery Utilities**:
   Provide `database/BACKUP_AND_RESTORE.md` and `scripts/backup-database.bat`.
5. **Run Full Regression Suite**:
   Execute all 85 tests across backend, frontend, E2E, smoke, security, integration, and performance.
6. **Generate Deliverables & Release**:
   Prepare complete academic architecture diagrams, documentation, commit to Git, and push to GitHub `main`.
