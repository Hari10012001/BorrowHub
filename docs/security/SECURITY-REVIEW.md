# BorrowHub — Security Architecture & Vulnerability Review

This document reports the security posture, threat model, and automated security audit results for **BorrowHub**.

---

## 1. Security Architecture Overview

BorrowHub employs defense-in-depth principles across all architectural tiers:

```
                      [ Incoming HTTP Request ]
                                 │
                                 ▼
                     [ CORS Policy Evaluation ]
           (Controlled origin patterns & exposed headers)
                                 │
                                 ▼
              [ Spring Security 6 Stateless Filter ]
              - Extracts 'Authorization: Bearer <JWT>'
              - Cryptographic signature check (HMAC-SHA256)
              - Rejection of expired or tampered tokens
                                 │
                                 ▼
             [ RFC 8785 Idempotency Key Gatekeeper ]
       - Canonical request fingerprinting (endpoint + user + payload)
       - Strict tenant scoping: Prevents cross-user key theft
                                 │
                                 ▼
             [ Method & Endpoint Authorization (RBAC) ]
       - ADMIN: Full administrative, inventory, and circulation access
       - FACULTY: Elevated borrow duration & desk checkout access
       - STUDENT: Standard catalog, reservation, and loan access
                                 │
                                 ▼
              [ Database Security & Query Isolation ]
       - Zero dynamic SQL string concatenation (Hibernate parameters)
       - BCrypt password hashing (Cost factor 12)
       - Dedicated non-root MySQL application credentials
```

---

## 2. Vulnerability Assessment & Findings Matrix

| Finding ID | Security Category | Severity | Description | Current Status | Mitigation / Verification |
|---|---|---|---|---|---|
| **SEC-F01** | Privilege Escalation | **High** | Public registration assigning privileged roles (`ADMIN`, `FACULTY`) | **RESOLVED** | Public registration enforces `Role.STUDENT` regardless of request payload; verified by `security-check.ps1`. |
| **SEC-F02** | Credential Storage | **High** | Plaintext or weakly hashed user passwords in database | **RESOLVED** | All passwords stored using BCrypt adaptive hashing with cost factor 12 (`$2a$12$...`); verified by database inspection. |
| **SEC-F03** | Token Tampering | **Medium** | Modification of JWT payload or signature | **RESOLVED** | JJWT parser strictly validates cryptographic HMAC signature and token expiration; returns HTTP 401 on tampering. |
| **SEC-F04** | SQL Injection | **High** | Untrusted user input in search or query parameters | **RESOLVED** | All queries utilize Spring Data JPA parameter binding and parameterized prepared statements; verified by SQL injection probes. |
| **SEC-F05** | Sensitive Info Leakage | **Medium** | Passwords or cryptographic hashes exposed in API responses | **RESOLVED** | DTO projections (`UserProfileResponse`, `AuthResponse`) omit password and salt fields; verified by profile inspection. |
| **SEC-F06** | Idempotency Hijacking | **Medium** | User B reusing idempotency key previously claimed by User A | **RESOLVED** | Fingerprint includes authenticated user ID; cross-user reuse returns HTTP 400 `BadRequestException`; verified by `CanonicalJsonAndIdempotencyTests`. |
| **SEC-F07** | CSRF Vulnerability | **Low** | Cross-Site Request Forgery on state-changing requests | **NOT APPLICABLE** | Application uses stateless JWT authentication via `Authorization: Bearer` headers (not ambient cookies); CSRF is disabled per Spring Security best practices. |
| **SEC-F08** | CORS Misconfiguration | **Low** | Overly permissive origin policies | **RESOLVED** | CORS configuration permits standard local ports (`5173`, `3000`) and restricts allowed methods and headers. |

---

## 3. Severity Summary

| Severity Level | Total Identified | Total Resolved | Remaining Unmitigated |
|---|---|---|---|
| **Critical** | 0 | 0 | **0** |
| **High** | 3 | 3 | **0** |
| **Medium** | 3 | 3 | **0** |
| **Low / Info** | 2 | 2 | **0** |
| **TOTAL** | **8** | **8** | **0** |

**Conclusion:** Zero critical or high vulnerabilities remain. The platform satisfies enterprise application security requirements for institutional deployment.
