# Changelog

All notable changes to the **BorrowHub** platform are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.0] - 2026-11-12

### Initial Full Stack Production-Ready Release (Architecture Baseline v2.4.8)

#### Added
- **Core Architecture & Invariants:**
  - Resolved 50 critical audit findings (**C1 through C50**) across transactional concurrency, idempotency, database arbiter clocks, and relational constraints.
  - Strict **Directed Acyclic Graph (DAG) Global Lock Ordering** (Levels 0 through 6) to eliminate deadlocks under high concurrency (C36, C43).
  - **Authoritative Central Database Clock Arbiter** (`Asia/Kolkata` `+05:30`) enforcing zero JVM business clock usage (C40, C42).
  - **STORED Generated Columns** in MySQL (`quantity_outstanding`) with automatic JPA entity synchronization (C17).
  - **RFC 8785 JSON Canonicalization Scheme (JCS)** via `org.erdtman.jcs.JsonCanonicalizer` with SHA-256 fingerprinting for multi-tenant idempotency (C23, C26).
  - **Durable Reservation Expiry** executed and committed in dedicated `Propagation.REQUIRES_NEW` transactions (C21).
  - **Partial Checkout Management** with immediate remainder release and `STOCK_REPLENISHED` outbox notifications (C30, C38).
  - **Transactional Outbox Engine** featuring Just-in-Time (JIT) single-claim `SKIP LOCKED` and conditional worker fencing generations (C47, C48, C49).
  - **Spring Proxy Non-Transactional Retry Orchestrator** isolating `@Retryable` aspects from transaction boundaries (C50).
- **Backend Components:**
  - 17 relational database tables managed by Flyway (`V1__initial_schema.sql`).
  - 15 domain enums and 17 JPA persistence entities.
  - 17 Spring Data JPA repositories with pessimistic locking semantics.
  - Spring Security 6 stateless JWT authentication and role-based access control (`ADMIN`, `FACULTY`, `STUDENT`).
  - Actuator `/actuator/health` endpoint enabled for operational monitoring.
  - Domain event outbox dispatchers and schedulers.
- **Frontend Components:**
  - Single Page Application built with React 19 / 18, Vite 8.3, and Tailwind CSS 3.4.
  - 9 fully functional views: Login, Register, Catalog, Resource Detail, My Borrowings, My Reservations, My Waitlist, Circulation Desk, and Pending Approvals.
  - Real-time inventory status badges, responsive layout, modal dialogs, and error alerts.
- **Automation & Quality Assurance:**
  - Complete suite of Windows `.bat` automation scripts in `scripts/` with process tracking and health polling.
  - Automated test suite covering architecture, concurrency, idempotency, and security (13/13 passing tests).
  - Pre-seeded test accounts for instant role-based validation.
