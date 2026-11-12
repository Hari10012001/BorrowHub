# BorrowHub — Project Structure & Architectural Layers

This document details the organizational layout, module boundaries, and design patterns across the **BorrowHub** platform.

---

## 1. Repository Directory Structure

```
BorrowHub/
├── README.md                          # Master documentation & quickstart
├── CHANGELOG.md                       # Release notes and version history
├── CONTRIBUTING.md                    # Contribution guidelines & concurrency rules
├── .gitignore                         # Git exclusion rules
├── .editorconfig                      # Universal code formatting rules
├── .env.example                       # Environment variable reference
│
├── scripts/                           # Windows .bat automation scripts
│   ├── start-borrowhub.bat            # Full stack 1-click startup
│   ├── stop-borrowhub.bat             # Graceful process shutdown
│   ├── restart-borrowhub.bat          # Clean service restart
│   ├── start-database.bat             # MySQL daemon starter
│   ├── stop-database.bat              # MySQL daemon stopper
│   ├── database-status.bat            # Database inspection
│   ├── open-database.bat              # Interactive database console
│   ├── open-borrowhub.bat             # Browser launcher
│   ├── health-check.bat               # Multi-tier health verification
│   ├── run-backend-tests.bat          # Backend test executor
│   ├── run-frontend-tests.bat         # Frontend test & build executor
│   ├── run-e2e-tests.bat              # Playwright browser test executor
│   └── run-full-qa.bat                # Master QA pipeline orchestrator
│
├── docs/                              # Project documentation
│   ├── architecture/                  # Architectural specs and patterns
│   ├── database/                      # Schema design, constraints, ERDs
│   ├── api/                           # REST API catalog and payload specs
│   ├── setup/                         # Workstation setup & script guides
│   ├── testing/                       # QA strategy, reports, manual checklist
│   ├── security/                      # Threat model and vulnerability audit
│   └── reports/                       # Formal QA execution records
│
├── database/                          # Database resources
│   ├── README.md                      # Database documentation
│   ├── backups/                       # Database backup targets
│   ├── test-data/                     # QA test seed datasets
│   └── scripts/                       # Maintenance scripts
│
├── borrowhub-backend/                 # Spring Boot 3.4.1 (Java 21 LTS)
│   ├── pom.xml                        # Maven dependencies & plugins
│   ├── README.md                      # Backend service guide
│   └── src/
│       ├── main/java/com/borrowhub/
│       │   ├── BorrowHubApplication.java
│       │   ├── config/                # Spring configuration beans
│       │   ├── controller/            # REST endpoints
│       │   ├── exception/             # Centralized exception handlers
│       │   ├── model/entity/          # 17 JPA persistence entities
│       │   ├── model/enums/           # 15 Domain enums
│       │   ├── repository/            # 17 JPA repositories (DAG locks)
│       │   ├── scheduler/             # Outbox, expiry, overdue cron jobs
│       │   ├── security/              # JWT provider & RBAC filters
│       │   ├── service/               # Core business services
│       │   │   └── outbox/            # Domain event listeners
│       │   └── util/                  # RFC 8785 JSON canonicalizer
│       └── test/java/com/borrowhub/   # Automated JUnit 5 test suites
│
├── borrowhub-frontend/                # React 19 / Vite / Tailwind
│   ├── package.json                   # Frontend dependencies & scripts
│   ├── vite.config.js                 # Vite & Vitest configuration
│   ├── tailwind.config.js             # Tailwind design system
│   ├── index.html                     # Root HTML document
│   ├── README.md                      # Frontend application guide
│   ├── e2e/                           # Playwright end-to-end tests
│   └── src/
│       ├── api/                       # Axios client & JWT interceptors
│       ├── components/                # Reusable UI (Navbar, Sidebar, Modal, Badge)
│       ├── context/                   # React Context API (AuthContext)
│       ├── pages/                     # 9 Functional application views
│       └── test/                      # Vitest unit & component tests
│
├── qa/                                # Quality Assurance Framework
│   ├── smoke/                         # Automated smoke tests
│   ├── e2e/                           # Playwright test specs
│   ├── security/                      # Automated vulnerability scanner
│   ├── performance/                   # Latency & throughput benchmark
│   ├── accessibility/                 # a11y & responsive audit
│   ├── test-data/                     # Mock data & payloads
│   └── reports/                       # Full QA timestamped reports
│
├── runtime/                           # Process metadata (PIDs) - gitignored
├── logs/                              # Service execution logs - gitignored
└── temp/                              # Temporary test artifacts - gitignored
```

---

## 2. Architectural Layering

BorrowHub adheres to clean separation of concerns across four discrete layers:

```
┌─────────────────────────────────────────────────────────────┐
│ 1. Presentation Layer (React 19 / Vite / Tailwind)          │
│    - Role-based views: Student, Faculty, Admin              │
│    - Axios API client with automatic JWT bearer inclusion   │
└──────────────────────────────┬──────────────────────────────┘
                               │ HTTP / JSON (REST)
                               ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. Security & Gateway Layer (Spring Security 6)              │
│    - Stateless JWT authentication filter                    │
│    - RBAC: ADMIN, FACULTY, STUDENT                          │
│    - RFC 8785 Canonical JSON request fingerprinting (C23)   │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. Domain & Service Layer (Spring Boot 3.4.1)               │
│    - Directed Acyclic Graph (DAG) Lock Hierarchy (C36, C43) │
│    - Isolated @Transactional(REQUIRES_NEW) Expiry (C21)     │
│    - Non-transactional Spring Retry Orchestrator (C50)      │
│    - Transactional Outbox Single-Claim Dispatcher (C47–C49) │
└──────────────────────────────┬──────────────────────────────┘
                               │ JDBC / HikariCP
                               ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. Persistence & Database Layer (MySQL 8.0.43 InnoDB)       │
│    - Sole Time Arbiter: CURRENT_DATE() / NOW() (C40, C42)   │
│    - STORED Generated Column: quantity_outstanding (C17)    │
│    - Check Constraints: chk_br_terminal_consistency (C13)   │
│    - 17 Relational Tables managed by Flyway                 │
└─────────────────────────────────────────────────────────────┘
```
