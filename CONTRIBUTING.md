# Contributing to BorrowHub

Thank you for contributing to **BorrowHub — A Smart Resource Borrowing, Reservation and Inventory Management Platform**.

To maintain the architectural integrity, concurrency safety, and code quality of this enterprise platform, please adhere to the following contribution standards.

---

## 1. Code of Conduct & Core Invariants

BorrowHub enforces strict concurrency and database rules. Every pull request or change must preserve the following architectural guarantees:

1. **DAG Lock Ordering (Levels 0–6):**
   - Never invert the lock acquisition order. Always acquire locks strictly in ascending order:
     - `Level 0`: `idempotency_keys`
     - `Level 1`: `resources` (ascending ID order)
     - `Level 2`: `resource_units` (ascending ID order)
     - `Level 3`: `borrowing_requests` (ascending ID order)
     - `Level 4`: `reservations` (ascending ID order)
     - `Level 5`: `waitlist_entries` (ascending ID order)
     - `Level 6`: `borrowing_records` (ascending ID order)
2. **Zero JVM Business Clock Usage (C42):**
   - Never call `LocalDate.now()` or `LocalDateTime.now()` for business timestamps or status dates. All state timestamps must be evaluated by MySQL `CURRENT_DATE()` or `NOW()` in `+05:30` timezone.
3. **STORED Generated Columns (C17):**
   - Never write directly to `quantity_outstanding`. Always mutate source columns `quantity_issued`, `quantity_returned`, or `quantity_lost` and refresh/reload the entity.
4. **RFC 8785 Idempotency (C23, C26):**
   - State-changing circulation and request endpoints must pass requests through `IdempotencyService` and compute fingerprints using `CanonicalJsonUtil`.
5. **No Broken Windows:**
   - Never commit code that breaks `mvn test` or `npm run build`.

---

## 2. Development Setup

### Prerequisites
- JDK 21 LTS (`java -version`)
- Maven 3.9+ (`mvn -version`)
- Node.js 18+ and npm (`node -v`, `npm -v`)
- MySQL 8.0 Server running on port 3306

### Starting Local Services
Use the provided Windows automation scripts:
```cmd
scripts\start-borrowhub.bat
```
Or start manually:
```cmd
# Terminal 1: Backend
cd borrowhub-backend
mvn spring-boot:run

# Terminal 2: Frontend
cd borrowhub-frontend
npm run dev
```

---

## 3. Pull Request Guidelines

1. **Branch Naming:**
   - `feature/<short-description>`
   - `fix/<short-description>`
   - `test/<short-description>`
2. **Testing Requirements:**
   - All backend tests must pass: `scripts\run-backend-tests.bat`
   - All frontend tests and lint must pass: `scripts\run-frontend-tests.bat`
   - End-to-end user journeys must pass: `scripts\run-e2e-tests.bat`
3. **Commit Messages:**
   - Use conventional commit format:
     - `feat: add physical unit barcode scanner support`
     - `fix: resolve stale balance projection in circulation return`
     - `test: add concurrent waitlist promotion test`
