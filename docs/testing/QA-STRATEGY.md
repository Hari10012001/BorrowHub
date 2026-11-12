# BorrowHub — Complete Quality Assurance Strategy

This document specifies the multi-tier Quality Assurance (QA) strategy implemented for **BorrowHub**.

---

## 1. QA Objectives & Guarantees

BorrowHub enforces real, empirical verification. Testing is not limited to compilation or happy paths; it validates concurrency guarantees, race condition immunity, database integrity constraints, cryptographic idempotency, and end-to-end user journeys.

### Core Testing Pillars:
1. **Zero Fake Results:** Tests must run against live or properly isolated environments; results reflect actual process execution.
2. **Defect Fix Loop:** Discovered defects are reproduced, isolated, fixed, and accompanied by regression assertions.
3. **Multi-Tier Quality Gates:** Verification spans unit, integration, browser E2E, security, performance, accessibility, and operational health.

---

## 2. Test Execution Tiers

```
┌─────────────────────────────────────────────────────────────┐
│ Tier 1: Unit & Component Tests                              │
│ - Backend: Mockito Service Tests (AvailabilityService)       │
│ - Frontend: Vitest + React Testing Library (Badge, Modal)   │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│ Tier 2: Integration & Architecture Tests                    │
│ - Flyway Schema Migrations & Constraints against MySQL 8.0  │
│ - RFC 8785 Canonical JSON & Idempotency Tests (C23, C26)    │
│ - Circulation Concurrency & DAG Lock Order Tests (C36, C43) │
│ - Database Clock Arbiter & Stored Column Tests (C17, C42)   │
│ - Durable Reservation Expiry (C21)                          │
│ - Spring MockMvc Controller & Security Tests                │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│ Tier 3: Browser End-to-End Tests (Playwright)               │
│ - Headless Chromium execution across 6 primary journeys:    │
│   1. Student Authentication & Navigation                     │
│   2. Invalid Password Error Feedback                        │
│   3. Resource Catalog Search & Filtering                    │
│   4. RBAC Protection (Student blocked from Admin Desk)      │
│   5. Admin Circulation Desk Access                          │
│   6. User Logout & Session Invalidation                     │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│ Tier 4: Security, Performance & Operational Audits          │
│ - Automated Security Scanner (SQLi, XSS, Escaped Roles)     │
│ - API Latency & Throughput Benchmark (Stopwatch Metrics)    │
│ - Accessibility (a11y) & Semantic Landmark Audit            │
│ - Multi-tier System Health Check (Ports, Actuator, Web)     │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. Automation Commands Reference

| Test Suite | Automation Command | Framework / Tool |
|---|---|---|
| **Backend Architecture & Unit Tests** | `scripts\run-backend-tests.bat` | JUnit 5, Spring Boot Test, AssertJ |
| **Frontend Tests & Build** | `scripts\run-frontend-tests.bat` | Vitest, React Testing Library, Vite |
| **Playwright E2E Tests** | `scripts\run-e2e-tests.bat` | Playwright, Headless Chromium |
| **Operational Smoke Tests** | `powershell -File qa\smoke\smoke-test.ps1` | PowerShell REST Client |
| **Security Audit** | `powershell -File qa\security\security-check.ps1` | Automated Security Probes |
| **Performance Benchmark** | `powershell -File qa\performance\perf-test.ps1` | High-Resolution Stopwatch Metrics |
| **Accessibility Audit** | `powershell -File qa\accessibility\a11y-check.ps1` | Semantic HTML & Meta Tag Validator |
| **Full Automated QA Pipeline** | `scripts\run-full-qa.bat` | Master Orchestrator |

---

## 4. Defect Management & Fix Protocol

When any test fails during local development or CI/CD execution:
1. **Reproduce:** Run the specific test class or spec file in isolation.
2. **Diagnose:** Inspect the stack trace, error context, or Playwright failure screenshot in `borrowhub-frontend/test-results/`.
3. **Fix Root Cause:** Modify production code or database constraints rather than disabling assertions or adding broad catch blocks.
4. **Regression Coverage:** Ensure existing or newly added tests verify the fix.
5. **Full Suite Re-run:** Execute `scripts\run-full-qa.bat` to confirm zero unintended side effects.
