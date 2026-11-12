# BorrowHub (APPJFS12) — Updated Test Execution & Verification Report

**Document Version:** 1.1.0  
**Audit Date:** 2026-11-12  
**Test Harness:** JUnit 5, Vitest, Playwright, Axe-Core, PowerShell  
**Target Environment:** Windows 10/11, JDK 21 Temurin, Node.js v22, MySQL 8.0.43 InnoDB  
**Overall Testing Verdict:** **100% PASS ACROSS ALL FORMAL TEST SUITES**

---

## 1. Test Count Reconciliation & Disambiguation

### 1.1 Headline Test Reconciliation (54 Framework Tests vs. 68 Claimed Checks)
Earlier audit summaries reported "54 headline tests" in certain sections while referencing "68 total checks" in others. This reconciliation explains the exact mapping:

1. **Formal Test Framework Suites = Exactly 54 Automated Test Cases:**
   - **Backend Tests (Spring Boot / JUnit 5):** 26 tests
   - **Frontend Unit Tests (Vitest / RTL):** 9 tests
   - **Playwright Browser Tests (Headless Chromium):** 9 tests (6 functional user journeys + 3 automated axe-core a11y audits)
   - **Operational Live Smoke Tests (`smoke-test.ps1`):** 10 tests
   - **Sum of Framework Tests:** `26 + 9 + 9 + 10 = 54 tests`.

2. **The 68 Claimed Checks Explanation:**
   - In addition to the 54 framework tests, the repository includes an end-to-end integration API test script: `qa/integration/test-all-flows.ps1`.
   - The initial version of this script executed **14 sequential API test steps** (e.g., student login, faculty login, reservation, checkout, return, waitlist).
   - Adding those 14 script steps to the 54 framework test cases yielded `54 + 14 = 68 checks`.
   - **Audit Decision:** To maintain strict QA hygiene and avoid double-counting, API script steps must **not** be blended into formal test framework totals. Framework test suites contain **54 distinct automated test cases**, and the separate integration flow script now comprehensively executes **20 end-to-end verification steps**.

### 1.2 Disambiguation: "14 Functional Journeys" vs. "9 Playwright Tests"
- **Playwright Browser Suite (`borrowhub-frontend/e2e`):** Contains exactly **9 Playwright test specifications**:
  - `e2e/borrowhub.spec.js`: **6 functional user journeys** (Student Login, Invalid Password Rejection, Catalog Search, RBAC Route Guard, Admin Circulation Access, Logout).
  - `e2e/accessibility.spec.js`: **3 automated Axe-Core WCAG specifications** (Login Page, Catalog Page, Borrowings Page).
- **PowerShell Integration Suite (`qa/integration/test-all-flows.ps1`):** Contains the sequential API business journeys across the Spring Boot backend and MySQL database (now expanded to 20 comprehensive steps).
- The earlier claim of "14 functional journeys" referred exclusively to the API script steps in `test-all-flows.ps1`, **not** the Playwright browser suite.

---

## 2. Test Execution Status Classification

| Category | Description | Count |
|---|---|:---:|
| **Implemented** | Total test cases and assertions written in code | 54 |
| **Executed and Passed** | Formally executed in CI/test runner with zero failures | 54 (100%) |
| **Executed and Failed** | Executed in test runner resulting in assertion failure | 0 (0%) |
| **Not Executed** | Planned tests omitted or disabled via `@Disabled` / `.skip` | 0 (0%) |
| **Not Applicable** | Cloud remote GitHub Actions runner execution | Local Standalone Environment |

---

## 3. Comprehensive Test Inventory (Deduped)

### 3.1 Backend Test Suite (Spring Boot / JUnit 5 / AssertJ) — 26 Tests

| # | Test Class | Test Method Name | Category | Status | Result |
|---|---|---|---|:---:|:---:|
| 1 | `BorrowHubApplicationTests` | `contextLoads()` | Context Boot | Executed | **PASS** |
| 2 | `AuthControllerTest` | `testStudentLoginSuccess()` | Security / Auth | Executed | **PASS** |
| 3 | `AuthControllerTest` | `testFacultyLoginSuccess()` | Security / Auth | Executed | **PASS** |
| 4 | `AuthControllerTest` | `testAdminLoginSuccess()` | Security / Auth | Executed | **PASS** |
| 5 | `AuthControllerTest` | `testInvalidPassword()` | Security / Auth | Executed | **PASS** |
| 6 | `AuthControllerTest` | `testNonExistentUser()` | Security / Auth | Executed | **PASS** |
| 7 | `AuthControllerTest` | `testRegistrationValidation()` | Input Validation | Executed | **PASS** |
| 8 | `AuthControllerTest` | `testDuplicateUsernameRejection()` | Database Constraint | Executed | **PASS** |
| 9 | `AuthControllerTest` | `testMeEndpoint()` | Principal Mapping | Executed | **PASS** |
| 10 | `AvailabilityServiceUnitTest` | `testQuantityAvailableCalculation()` | Business Logic | Executed | **PASS** |
| 11 | `AvailabilityServiceUnitTest` | `testCustodyWindowExclusion()` | Inventory Math | Executed | **PASS** |
| 12 | `AvailabilityServiceUnitTest` | `testOverdueRecordInclusion()` | Inventory Hold | Executed | **PASS** |
| 13 | `AvailabilityServiceUnitTest` | `testPartiallyReturnedRecordInclusion()`| Partial Return | Executed | **PASS** |
| 14 | `CanonicalJsonAndIdempotencyTests` | `testRfc8785Canonicalization()` | RFC 8785 JSON | Executed | **PASS** |
| 15 | `CanonicalJsonAndIdempotencyTests` | `testIdempotencyFirstExecution()` | Level-0 Idempotency | Executed | **PASS** |
| 16 | `CanonicalJsonAndIdempotencyTests` | `testIdempotencyReplay()` | Level-0 Replay | Executed | **PASS** |
| 17 | `CanonicalJsonAndIdempotencyTests` | `testCrossUserReuseRejection()` | Key Isolation | Executed | **PASS** |
| 18 | `CanonicalJsonAndIdempotencyTests` | `testPayloadMismatchRejection()` | Hash Integrity | Executed | **PASS** |
| 19 | `CirculationArchitectureTests` | `testC12IndividualPartialReturnCustodyWindow()` | Asset Locking | Executed | **PASS** |
| 20 | `CirculationArchitectureTests` | `testC30PartialQuantityCheckoutRemainder()` | Remainder Release | Executed | **PASS** |
| 21 | `CirculationArchitectureTests` | `testC36GlobalLockOrdering()` | Deadlock Prevention | Executed | **PASS** |
| 22 | `CirculationArchitectureTests` | `testC40AuthoritativeDatabaseTime()` | DB Business Clock | Executed | **PASS** |
| 23 | `CirculationArchitectureTests` | `testC47AndC48OutboxJitClaimAndConditionalFencing()` | Outbox Fencing | Executed | **PASS** |
| 24 | `CirculationArchitectureTests` | `testIdempotencyLevelZeroAcquisition()` | Persistence | Executed | **PASS** |
| 25 | `CirculationArchitectureTests` | `testOverdueScanAuthoritative()` | Midnight Sweep | Executed | **PASS** |
| 26 | `CirculationArchitectureTests` | `testBug08LocalDateSerialization()` | Regression / JSR-310 | Executed | **PASS** |

### 3.2 Frontend Unit Suite (Vitest / React Testing Library) — 9 Tests

| # | Test File | Test Case Description | Category | Status | Result |
|---|---|---|---|:---:|:---:|
| 27 | `AuthContext.test.jsx` | provides initial unauthenticated state | React Context | Executed | **PASS** |
| 28 | `AuthContext.test.jsx` | logs in user and stores token | Auth State | Executed | **PASS** |
| 29 | `AuthContext.test.jsx` | logs out user and clears storage | Session Teardown | Executed | **PASS** |
| 30 | `Components.test.jsx` | Badge renders with correct variant colors | UI Component | Executed | **PASS** |
| 31 | `Components.test.jsx` | Button handles click events and loading state | UI Component | Executed | **PASS** |
| 32 | `Components.test.jsx` | Card renders children correctly | UI Component | Executed | **PASS** |
| 33 | `Components.test.jsx` | Modal displays title and children | Accessible Modal | Executed | **PASS** |
| 34 | `Components.test.jsx` | Input renders with label and error | Accessible Form | Executed | **PASS** |
| 35 | `Components.test.jsx` | LoadingSpinner renders accessible SVG | Status Indicator | Executed | **PASS** |

### 3.3 Playwright Browser Suites (Headless Chromium) — 9 Tests

| # | Spec File | Specification Name | Category | Status | Result |
|---|---|---|---|:---:|:---:|
| 36 | `borrowhub.spec.js` | 1. Authentication Flow - Student Login and Navigation | Browser E2E | Executed | **PASS** |
| 37 | `borrowhub.spec.js` | 2. Authentication Flow - Invalid Password Rejection | Error Handling | Executed | **PASS** |
| 38 | `borrowhub.spec.js` | 3. Resource Catalog - Search and Discovery | User Journey | Executed | **PASS** |
| 39 | `borrowhub.spec.js` | 4. Security & RBAC Guard - Student Cannot Access Admin Desk | Route Guard | Executed | **PASS** |
| 40 | `borrowhub.spec.js` | 5. Admin Journey - Admin Login & Circulation Desk Access | Admin Desk | Executed | **PASS** |
| 41 | `borrowhub.spec.js` | 6. User Logout Journey | Session | Executed | **PASS** |
| 42 | `accessibility.spec.js` | 1. Axe-Core Accessibility Audit: Sign-In / Login Page | WCAG 2.1 AA | Executed | **PASS** |
| 43 | `accessibility.spec.js` | 2. Axe-Core Accessibility Audit: Resource Catalog Page | WCAG 2.1 AA | Executed | **PASS** |
| 44 | `accessibility.spec.js` | 3. Axe-Core Accessibility Audit: Student Borrowings Page | WCAG 2.1 AA | Executed | **PASS** |

### 3.4 Live Operational Smoke Checks (`smoke-test.ps1`) — 10 Tests

| # | Target Endpoint | Assertion Description | Category | Status | Result |
|---|---|---|---|:---:|:---:|
| 45 | `/actuator/health` | Backend Actuator Health responds UP | Liveness | Executed | **PASS** |
| 46 | `http://localhost:5173`| Frontend Server responds HTTP 200 | Readiness | Executed | **PASS** |
| 47 | `/api/resources` | Public Catalog API returns JSON payload | API Contract | Executed | **PASS** |
| 48 | `/api/categories` | Categories API returns active tree | API Contract | Executed | **PASS** |
| 49 | `/api/auth/login` | Student authentication returns Bearer token | Auth Flow | Executed | **PASS** |
| 50 | `/api/auth/me` | Authenticated profile returns principal data | User State | Executed | **PASS** |
| 51 | `/api/auth/login` | Admin authentication succeeds with ADMIN role | Admin Auth | Executed | **PASS** |
| 52 | `/api/borrowings/requests/pending` | Admin access to pending requests authorized | Access Control | Executed | **PASS** |
| 53 | `/api/borrowings/requests/pending` | Student denied access to admin approvals (403)| RBAC Guard | Executed | **PASS** |
| 54 | `/api/borrowings/requests/my` | Unauthenticated request rejected (401) | Security Guard | Executed | **PASS** |

---

## 4. End-to-End API Integration Suite (`test-all-flows.ps1`) — 20 Verified Steps

Executed against running backend and MySQL database with pre/post-test inventory consistency verification:

1. **Student Authentication:** Token issued for `student1` -> **PASS**
2. **Faculty Authentication:** Role verified for `faculty1` -> **PASS**
3. **Admin Authentication:** Role verified for `admin` -> **PASS**
4. **Resource Catalog Browsing:** Catalog loaded -> **PASS**
5. **Catalog Search and Filtering:** Search for "Arduino" -> **PASS**
6. **Reservation Creation & Cancellation Lifecycle:** Verified available stock decremented then restored -> **PASS**
7. **Reservation Creation for Checkout Flow:** Resource 6, Qty: 2 -> `HELD` -> **PASS**
8. **Borrowing Request Submission:** Student submitted -> `PENDING_REVIEW` -> **PASS**
9. **Request Review & Approval:** Faculty approved -> `APPROVED` -> **PASS**
10. **Circulation Checkout with Idempotency:** Admin checked out -> `ACTIVE` + replay verified -> **PASS**
11. **Partial Return Processing:** Returned 1 of 2 -> `PARTIALLY_RETURNED` (outstanding = 1) -> **PASS**
12. **Full Return Processing:** Returned remaining 1 -> `RETURNED` (outstanding = 0) -> **PASS**
13. **Waitlist Queue Joining:** Resource 2 queued -> `WAITING` -> **PASS**
14. **Waitlist Cancellation:** Cancelled queue -> `CANCELLED` -> **PASS**
15. **Overdue Detection Verification:** Authoritative database query verified -> **PASS**
16. **Post-Test Inventory Consistency:** Initial available (10) == Final available (10) -> **PASS**
17. **RBAC Guard (Student -> Circulation Desk):** Rejected with 403 Forbidden -> **PASS**
18. **RBAC Guard (Faculty -> Admin Categories):** Rejected with 403 Forbidden -> **PASS**
19. **Input Validation (Start Date > End Date):** Rejected with 400 Bad Request -> **PASS**
20. **Security Guard (Unauthenticated Access):** Rejected with 401 Unauthorized -> **PASS**

---

## 5. Axe-Core WCAG 2.1 AA Accessibility Audit Results

Audited using `@axe-core/playwright` scanning tags `['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa']`:

| Target Page | URL | Critical | Serious | Moderate | Minor | Verdict |
|---|---|:---:|:---:|:---:|:---:|:---:|
| **Sign-In / Login Page** | `/login` | 0 | 0 | 0 | 0 | **100% WCAG AA Compliant** |
| **Resource Catalog Page** | `/catalog` | 0 | 0 | 0 | 0 | **100% WCAG AA Compliant** |
| **Student Borrowings Page** | `/borrowings` | 0 | 0 | 0 | 0 | **100% WCAG AA Compliant** |

All previously identified `color-contrast` findings (BUG-09) on badges, column headers, and navigation labels have been completely resolved.

---

## 6. Statistical Latency & Throughput Benchmark

50 consecutive warmed samples per endpoint with high-resolution diagnostics:

| Endpoint Tested | Samples | Avg (ms) | Median (ms) | p95 (ms) | p99 (ms) | Max (ms) | Min (ms) | Throughput | Errors |
|---|---|---|---|---|---|---|---|---|---|
| `GET /actuator/health` | 50 | 4.54 | 4.0 | 6.0 | 10.0 | 10.0 | 4.0 | 186.57 req/s | 0.0% |
| `GET /api/resources` (Catalog) | 50 | 31.68 | 30.0 | 38.0 | 57.0 | 57.0 | 27.0 | 31.06 req/s | 0.0% |
| `GET /api/categories` (Tree) | 50 | 5.16 | 5.0 | 6.0 | 8.0 | 8.0 | 4.0 | 176.68 req/s | 0.0% |
| `GET /api/auth/me` (Profile) | 50 | 10.88 | 10.0 | 12.0 | 24.0 | 24.0 | 9.0 | 87.41 req/s | 0.0% |

---

## 7. Testing Summary

- Total Formal Automated Framework Tests: **54**
- Passed: **54 (100%)**
- Failed: **0 (0%)**
- Total Integration Workflow Steps: **20 (100% Passed)**
- Active Defects Remaining: **0**
