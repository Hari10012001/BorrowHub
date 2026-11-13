# Security Policy

## 1. Security Architecture Overview

BorrowHub (**APPJFS12**) is an institutional resource borrowing, reservation, and inventory management platform designed with defense-in-depth principles:

1. **Stateless Authentication & Session Management**:
   - Authentication is governed by Spring Security using industry-standard JSON Web Tokens (JWT) signed with HMAC-SHA256 (`HS256`).
   - Tokens carry minimal user identity claims (`sub`, `id`, `roles`, `iat`, `exp`).
   - User sessions are entirely stateless (`SessionCreationPolicy.STATELESS`), eliminating server-side session fixation and CSRF exposure on API endpoints.

2. **Credential Storage & Cryptographic Hashing**:
   - All user passwords are encrypted using adaptive **BCrypt** hashing with a work factor of 12 (`BCryptPasswordEncoder(12)`).
   - Plaintext passwords and hashes are never exposed via REST API responses; DTO mappers explicitly exclude credential fields (`UserProfileResponse`, `UserPrincipal`).
   - Development and seed credentials are strictly decoupled into environment variables (`BORROWHUB_SEED_*_PASSWORD`). The `.env` file is excluded from Git tracking via `.gitignore`.

3. **Role-Based Access Control (RBAC)**:
   - Platform operations enforce granular authorization via method security (`@PreAuthorize`) and URL security patterns:
     - `ROLE_STUDENT`: Catalog search, personal reservation creation, personal borrowing requests, waitlist queue joining.
     - `ROLE_FACULTY`: Elevated borrowing approvals, circulation review, academic departmental requests.
     - `ROLE_STAFF`: Equipment checkout, return intake, condition logging, damage assessment.
     - `ROLE_ADMIN`: Master resource catalog management, category configuration, administrative reporting, security policy administration.
   - Endpoint handlers strictly validate that non-administrative users can only access their own records (`userId` ownership checks), mitigating Insecure Direct Object References (IDOR).

4. **Concurrency & Database Transaction Safety**:
   - To prevent double-allocation and race conditions under concurrent requests, state transitions enforce a strict **Level 0–6 DAG Transaction Locking Hierarchy**.
   - Idempotency is enforced for all mutation operations (`CheckoutRequestDto`, `ReturnRequestDto`) using canonical RFC 8785 JSON digests (`SHA-256`) and atomic lease fencing (`chk_idempotency_lease`).

5. **SQL Injection Resistance**:
   - Data access is mediated through Spring Data JPA and Hibernate ORM utilizing strictly parameterized queries and prepared statements.
   - Dynamic sorting parameters are validated and mapped to entity properties to prevent SQL injection in `ORDER BY` clauses.

---

## 2. Supported Versions

Security updates and patches are actively maintained for the following versions:

| Version | Supported | Notes |
|---|---|---|
| `1.0.x` | :white_check_mark: | Active release branch (`main`) |
| `< 1.0.0` | :x: | Pre-release architecture baselines (v2.4.0–v2.4.8) |

---

## 3. Secret Management & Environment Security

- **Environment Files**: The root `.env` and `.env.local` files contain sensitive instance-specific parameters (`DB_PASSWORD`, `JWT_SECRET`, `BORROWHUB_SEED_*_PASSWORD`) and **must never be committed to version control**.
- **Template Configuration**: Developers should copy `.env.example` to `.env` and assign strong, randomly generated secrets for local instances:
  - `JWT_SECRET`: Minimum 256 bits (32+ characters) of high-entropy base64 string.
  - `DB_PASSWORD`: Strong database credentials conforming to MySQL password validation policy.
- **Continuous Integration**: In automated CI/CD pipelines (e.g. GitHub Actions), secrets are injected via ephemeral per-job environment variables and never logged to stdout.

---

## 4. Notice Regarding Historical Development Commits

Prior to commit `aa3b6ce`, earlier development iterations contained default demo credentials and placeholders within source files and commit messages. 

> [!WARNING]
> Any credentials present in historical commits prior to `1dd7de3` / `aa3b6ce` must be considered strictly **non-production development artifacts**. If any such credential was ever used on an external or production environment, it must be rotated immediately.

---

## 5. Reporting a Security Vulnerability

If you discover a security vulnerability in BorrowHub, please report it privately:

1. **Do not create a public GitHub issue.**
2. Send an email to the project security maintainer at `security@borrowhub.local` or contact the repository owner directly via GitHub Security Advisories.
3. Include the following details in your report:
   - Description of the vulnerability.
   - Steps to reproduce or proof-of-concept (PoC) payload.
   - Potential impact and affected endpoints/components.
   - Any suggested mitigations.
4. The maintainers will acknowledge receipt within 48 hours and provide an estimated timeline for remediation.
