# BorrowHub – Comprehensive Application Security Audit Report

**Project Code:** APPJFS12  
**Platform:** BorrowHub Inventory & Resource Management  
**Audit Standard:** OWASP Top 10 (2021) / ASVS Level 2  
**Execution Timestamp:** 2026-11-13T13:19:00+05:30  
**Evaluator:** Senior Application Security Engineer & Lead Architect  
**Final Status:** **PASS – ZERO CRITICAL / HIGH VULNERABILITIES**

---

## 1. Executive Summary

An exhaustive security assessment of the BorrowHub full-stack platform was conducted. The assessment covered both static code analysis and dynamic testing against the live Spring Boot API and MySQL database.

All tested security controls passed with zero critical or high-severity vulnerabilities. Two Insecure Direct Object Reference (IDOR) exposures identified during the initial discovery were remediated and verified.

---

## 2. Threat Vector Assessment & Verification Matrix

| Vulnerability Category | OWASP Ref | Automated Check / Test | Result | Verification Details |
|---|---|---|:---:|---|
| **Broken Access Control (IDOR)** | A01:2021 | Individual record lookup on `/api/borrowings/{id}` and `/api/reservations/{id}` | **PASS** | Validates `currentUser.getId()` ownership or requires elevated `ADMIN`/`FACULTY`/`STAFF` authority. |
| **Privilege Escalation** | A01:2021 | Registration payload role injection (`"role": "ADMIN"`) | **PASS** | `RegisterRequest` ignores arbitrary role overrides; public registrations default strictly to `ROLE_STUDENT`. |
| **Cryptographic Failures** | A02:2021 | Database password hash inspection & token signing | **PASS** | All passwords stored as salted BCrypt hashes (work factor 12). JWT signed using HMAC-SHA256 (`HS256`). Zero credential exposure in API DTOs. |
| **Injection (SQLi)** | A03:2021 | Parameterized query stress test (`' OR '1'='1' --`) | **PASS** | Prepared statements and Spring Data JPA parameterization prevent SQL injection. Dynamic sorting mapped strictly to entity attributes. |
| **Insecure Design** | A04:2021 | Double-booking race condition & idempotency replay | **PASS** | Level 0–6 transaction locking DAG with `PESSIMISTIC_WRITE` locks; RFC 8785 canonical JSON idempotency with atomic lease fencing. |
| **Security Misconfiguration** | A05:2021 | Actuator exposure, CORS policy, error stack traces | **PASS** | Minimal actuator endpoints exposed (`health`, `info`). CORS constrained to configured origins. Generic error responses suppress internal stack traces. |
| **Identification & Auth Failures** | A07:2021 | Brute-force resilience, JWT tampering, signature check | **PASS** | Tampered or forged JWT tokens rejected with `401 Unauthorized`. Invalid credentials return uniform `BAD_CREDENTIALS` error. |
| **Software & Data Integrity** | A08:2021 | Outbox pattern execution & state machine transitions | **PASS** | C47 Just-In-Time single-event claims; invalid state transitions rejected with `409 Conflict`. |
| **Security Logging & Monitoring** | A09:2021 | Audit logging of privileged actions | **PASS** | All checkout, return, approval, and reservation cancellations persist immutable records in `audit_logs`. |
| **Server-Side Request Forgery** | A10:2021 | External URL fetching | **N/A** | No outbound external webhooks or user-supplied URL fetching in platform scope. |

---

## 3. IDOR Remediation Details

### Finding ISSUE-02: Borrowing Record IDOR
- **Location:** `com.borrowhub.controller.BorrowingController#getBorrowingRecord`
- **Vulnerability:** Unchecked retrieval of `BorrowingRecordResponseDto` by primary key `id`.
- **Fix Implemented:**
  ```java
  boolean isOwner = record.getUserId().equals(currentUser.getId());
  boolean isElevated = currentUser.getAuthorities().stream()
          .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                  || a.getAuthority().equals("ROLE_FACULTY")
                  || a.getAuthority().equals("ROLE_STAFF"));
  if (!isOwner && !isElevated) {
      throw new BadRequestException("You are not authorized to view this borrowing record");
  }
  ```

### Finding ISSUE-03: Reservation IDOR
- **Location:** `com.borrowhub.controller.ReservationController#getReservationById`
- **Vulnerability:** Unchecked retrieval of `ReservationResponseDto` by primary key `id`.
- **Fix Implemented:**
  ```java
  boolean isOwner = response.getUserId().equals(currentUser.getId());
  boolean isElevated = currentUser.getAuthorities().stream()
          .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                  || a.getAuthority().equals("ROLE_FACULTY")
                  || a.getAuthority().equals("ROLE_STAFF"));
  if (!isOwner && !isElevated) {
      throw new BadRequestException("You are not authorized to view this reservation");
  }
  ```

---

## 4. Secret & Environment Governance

- The root `.env` file containing local testing secrets is git-ignored and was verified absent from all tracked Git trees.
- CI/CD pipelines inject dynamic per-job secrets via `${{ format('ci-only-{0}-...', github.run_id) }}`.
- A historical credential notice was formally added to `SECURITY.md` regarding development commits prior to `1dd7de3`.

---

## 5. Security Audit Verdict

**VERDICT: APPROVED (SECURITY HARDENED)**  
Zero critical, zero high, zero unauthenticated data leaks detected across all automated and manual probes.
