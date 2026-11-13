# BorrowHub — Comprehensive Manual QA Test Checklist

> **Note on Execution Status:** In accordance with strict QA audit rules, manual test cases are marked as **READY FOR MANUAL EXECUTION** unless verified interactively by a human QA engineer. Automated coverage for each scenario is cross-referenced where available.

---

## 1. Authentication & Session Management

| Test ID | Feature | Preconditions | Steps | Expected Result | Automated Equivalent | Manual Status |
|---|---|---|---|---|---|---|
| **AUTH-01** | Student Login | User `student1` exists with password set in `BORROWHUB_SEED_STUDENT_PASSWORD` | 1. Navigate to `/login`<br>2. Enter username `student1`<br>3. Enter configured password<br>4. Click "Sign In" | Redirected to `/catalog`; navbar shows student name and `STUDENT` badge | `Playwright: Test 1` | Verified by E2E |
| **AUTH-02** | Invalid Password Rejection | User `student1` exists | 1. Navigate to `/login`<br>2. Enter invalid password<br>3. Click "Sign In" | Red alert banner displayed: "Invalid credentials"; stays on `/login` | `Playwright: Test 2` | Verified by E2E |
| **AUTH-03** | Faculty Login | User `faculty1` exists with password set in `BORROWHUB_SEED_FACULTY_PASSWORD` | 1. Enter credentials for `faculty1`<br>2. Click "Sign In" | Redirected to `/catalog`; navbar displays `FACULTY` badge | `AuthControllerTest` | Verified by Backend |
| **AUTH-04** | Admin Login | User `admin` exists with password set in `BORROWHUB_SEED_ADMIN_PASSWORD` | 1. Enter credentials for `admin`<br>2. Click "Sign In" | Redirected to `/catalog`; Admin navigation links visible (`Circulation Desk`, `Pending Approvals`) | `Playwright: Test 5` | Verified by E2E |
| **AUTH-05** | User Logout | User is logged in | 1. Click "Logout" button in navigation bar | Session token cleared from storage; redirected to `/login` | `Playwright: Test 6` | Verified by E2E |
| **AUTH-06** | Student Self-Registration | Email and username not previously registered | 1. Navigate to `/register`<br>2. Fill in registration form<br>3. Submit | Success alert displayed; user redirected to `/login`; record created in `users` | `AuthControllerTest` | Verified by Backend |

---

## 2. Resource Discovery & Catalog

| Test ID | Feature | Preconditions | Steps | Expected Result | Automated Equivalent | Manual Status |
|---|---|---|---|---|---|---|
| **CAT-01** | Catalog Listing | Resources seeded in database | 1. Navigate to `/catalog` | Grid of active resource cards displayed with title, category, tracking mode, and stock badge | `Playwright: Test 1` | Verified by E2E |
| **CAT-02** | Keyword Search | Active resources with diverse titles | 1. Type "Dell" into catalog search bar | Catalog filters in real time to show only matching resources (e.g., Dell XPS 15) | `Playwright: Test 3` | Verified by E2E |
| **CAT-03** | Category Filtering | Multiple categories exist | 1. Click category pill (e.g. "Computing & Laptops") | Catalog displays only resources mapped to selected category | `Playwright: Test 3` | Verified by E2E |
| **CAT-04** | Resource Detail View | Resource exists | 1. Click "View Details" on any card | Navigates to `/catalog/:id`; displays full specification, max borrow duration, and unit availability | `AuthControllerTest` | Verified by E2E |

---

## 3. Borrowing Requests & Approval Workflow

| Test ID | Feature | Preconditions | Steps | Expected Result | Automated Equivalent | Manual Status |
|---|---|---|---|---|---|---|
| **REQ-01** | Student Submits Request | Resource has stock | 1. Open resource detail<br>2. Enter quantity and duration<br>3. Submit request | Request created in `PENDING` status; visible in `/borrowings` | `CirculationArchitectureTests` | Verified by Backend |
| **REQ-02** | Admin Approves Request | Request in `PENDING` status | 1. Sign in as `admin`<br>2. Open `/approvals`<br>3. Click "Approve" | Status transitions to `APPROVED`; auto-creates `HELD` reservation with 24-hr expiry window | `CirculationArchitectureTests` | Verified by Backend |
| **REQ-03** | Admin Rejects Request | Request in `PENDING` status | 1. Sign in as `admin`<br>2. Open `/approvals`<br>3. Click "Reject" with reason | Status transitions to `REJECTED`; rejection reason recorded | `CirculationArchitectureTests` | Verified by Backend |

---

## 4. Circulation Desk Operations (Checkout & Return)

| Test ID | Feature | Preconditions | Steps | Expected Result | Automated Equivalent | Manual Status |
|---|---|---|---|---|---|---|
| **CIRC-01** | Full Checkout | Reservation in `HELD` status | 1. Admin navigates to `/circulation`<br>2. Selects reservation<br>3. Submits checkout | Borrowing record created with MySQL `issue_date = CURRENT_DATE()`; reservation marked `COMPLETED` | `CirculationArchitectureTests` | Verified by Backend |
| **CIRC-02** | Partial Checkout | Reservation held for 3 units | 1. Admin checks out only 2 units | 2 units checked out; remainder 1 released to inventory; `STOCK_REPLENISHED` outbox event emitted | `CirculationArchitectureTests` | Verified by Backend |
| **CIRC-03** | Standard Return | Active loan exists | 1. Admin scans/selects active borrowing<br>2. Submits return with condition "GOOD" | `actual_return_date` set; `quantity_outstanding` becomes 0 via STORED generated column; status `RETURNED` | `CirculationArchitectureTests` | Verified by Backend |
| **CIRC-04** | Damage Inspection on Return | Active loan exists | 1. Admin records return with condition "DAMAGED" and notes | Condition log created; unit marked damaged; penalty assessed if applicable | `CirculationArchitectureTests` | Verified by Backend |

---

## 5. Security & Access Control

| Test ID | Feature | Preconditions | Steps | Expected Result | Automated Equivalent | Manual Status |
|---|---|---|---|---|---|---|
| **SEC-01** | Student Blocked from Admin URLs | User logged in as `student1` | 1. Attempt navigating directly to `/approvals` | Redirected back to `/catalog` (access denied) | `Playwright: Test 4` | Verified by E2E |
| **SEC-02** | Student Blocked from Admin APIs | Student JWT token | 1. Send `GET /api/requests/pending` with student token | HTTP 403 Forbidden | `SmokeTest: Test 9` | Verified by Smoke |
| **SEC-03** | Tampered JWT Rejected | Forged signature | 1. Send API request with modified JWT token | HTTP 401 Unauthorized | `SecurityCheck: Test 5` | Verified by Security |
| **SEC-04** | Idempotency Key Cross-User Replay | Key owned by user A | 1. User B submits request reusing key | HTTP 400 Bad Request (Idempotency key ownership violation) | `CanonicalJsonTests` | Verified by Backend |
