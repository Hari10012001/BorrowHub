# BorrowHub – Academic Architectural Diagrams & System Models

**Project Code:** APPJFS12  
**Standard:** IEEE 1471 / ISO/IEC 42010 Architecture Documentation  
**Platform Version:** 1.0.0  

---

## 1. System Architecture Diagram

```mermaid
flowchart TD
    subgraph ClientTier["Client Tier (Presentation Layer)"]
        Browser["Modern Web Browser (Chrome, Edge, Firefox)"]
        SPA["React 19 Single Page Application (Vite 8)"]
        AuthContext["Auth Context (JWT In-Memory State)"]
        AxiosClient["Axios Client (Bearer Interceptor)"]
        Browser --> SPA
        SPA --> AuthContext
        SPA --> AxiosClient
    end

    subgraph SecurityTier["Security & API Gateway Tier"]
        CorsFilter["CORS Configuration Filter"]
        JwtFilter["JwtAuthenticationFilter (Stateless HS256)"]
        AuthEntryPoint["JwtAuthenticationEntryPoint (401 Handler)"]
        AccessDenied["JwtAccessDeniedHandler (403 Handler)"]
    end

    subgraph ApplicationTier["Application Service Tier (Spring Boot 3.4 / Java 21)"]
        Controllers["REST Controllers (Resource, Reservation, Borrowing, Circulation Desk)"]
        Services["Business Services (CirculationOrchestrator, AvailabilityService, OutboxService)"]
        IdempotencyEngine["Idempotency Engine (RFC 8785 Canonical JSON & SHA-256)"]
        Schedulers["Background Schedulers (JIT Outbox Dispatcher, Overdue Sweeper, Expiry)"]
    end

    subgraph PersistenceTier["Data Persistence Tier"]
        Repositories["Spring Data JPA Repositories (Pessimistic Write Locking)"]
        HibernateORM["Hibernate 6.x ORM (Level 0-6 DAG Locking Hierarchy)"]
        HikariPool["HikariCP Connection Pool (Timezone: +05:30)"]
    end

    subgraph DatabaseTier["Database Tier (MySQL 8.0.43 InnoDB)"]
        MySQL["MySQL 8.0 InnoDB Engine"]
        AppTables["17 Domain Tables (Users, Resources, Reservations, Borrowings)"]
        CheckConstraints["15 Integrity Constraints & 36 Indexes"]
        FlywayHistory["flyway_schema_history"]
    end

    AxiosClient -->|"HTTP / REST (JSON)"| CorsFilter
    CorsFilter --> JwtFilter
    JwtFilter -.->|"Failure"| AuthEntryPoint
    JwtFilter -.->|"Forbidden"| AccessDenied
    JwtFilter -->|"Authenticated Principal"| Controllers
    Controllers --> Services
    Services <--> IdempotencyEngine
    Services --> Repositories
    Schedulers --> Services
    Repositories --> HibernateORM
    HibernateORM --> HikariPool
    HikariPool -->|"TCP/IP Port 3306"| MySQL
    MySQL --> AppTables
    MySQL --> CheckConstraints
    MySQL --> FlywayHistory
```

---

## 2. Use Case Diagram

```mermaid
flowchart LR
    subgraph Actors["System Actors"]
        Student["Student Actor"]
        Faculty["Faculty Actor"]
        Staff["Staff / Circulation Desk Actor"]
        Admin["System Administrator Actor"]
    end

    subgraph UseCases["Platform Core Use Cases"]
        UC1["UC-01: Browse & Search Resource Catalog"]
        UC2["UC-02: Reserve Available Inventory (Held)"]
        UC3["UC-03: Submit Borrowing Request"]
        UC4["UC-04: Join Waitlist Queue"]
        UC5["UC-05: Review & Approve Borrowing Request"]
        UC6["UC-06: Issue Equipment (Circulation Checkout)"]
        UC7["UC-07: Process Return (Full or Partial)"]
        UC8["UC-08: Log Condition & Damage Assessment"]
        UC9["UC-09: Manage Resources & Asset Tags"]
        UC10["UC-10: Configure Categories & Policies"]
    end

    Student --> UC1
    Student --> UC2
    Student --> UC3
    Student --> UC4

    Faculty --> UC1
    Faculty --> UC2
    Faculty --> UC3
    Faculty --> UC5

    Staff --> UC1
    Staff --> UC6
    Staff --> UC7
    Staff --> UC8

    Admin --> UC1
    Admin --> UC5
    Admin --> UC6
    Admin --> UC7
    Admin --> UC8
    Admin --> UC9
    Admin --> UC10
```

---

## 3. Entity Relationship Diagram (ERD)

```mermaid
erDiagram
    USERS ||--o{ RESERVATIONS : "places"
    USERS ||--o{ BORROWING_REQUESTS : "submits"
    USERS ||--o{ BORROWING_RECORDS : "holds"
    USERS ||--o{ WAITLIST_ENTRIES : "joins"
    USERS ||--o{ AUDIT_LOGS : "triggers"
    USERS ||--o{ NOTIFICATIONS : "receives"

    CATEGORIES ||--o{ RESOURCES : "classifies"

    RESOURCES ||--o{ RESOURCE_UNITS : "contains"
    RESOURCES ||--o{ RESERVATIONS : "targets"
    RESOURCES ||--o{ BORROWING_REQUESTS : "requests"
    RESOURCES ||--o{ BORROWING_RECORDS : "loans"
    RESOURCES ||--o{ WAITLIST_ENTRIES : "queued_for"

    RESOURCE_UNITS ||--o{ RESERVATION_UNITS : "allocated_to"
    RESOURCE_UNITS ||--o{ BORROWING_RECORDS : "issued_as"
    RESOURCE_UNITS ||--o{ MAINTENANCE_RECORDS : "undergoes"
    RESOURCE_UNITS ||--o{ CONDITION_LOGS : "inspected_in"

    RESERVATIONS ||--o{ RESERVATION_UNITS : "reserves"
    RESERVATIONS ||--o{ BORROWING_RECORDS : "originates"

    BORROWING_RECORDS ||--o{ RETURN_TRANSACTIONS : "settled_by"

    USERS {
        bigint id PK
        varchar username UK
        varchar email UK
        varchar password_hash
        varchar role
        varchar account_status
        varchar department
    }

    CATEGORIES {
        bigint id PK
        varchar name UK
        varchar code UK
        boolean is_active
    }

    RESOURCES {
        bigint id PK
        bigint category_id FK
        varchar name
        varchar model_number
        varchar inventory_mode
        int total_quantity
        int available_quantity
        boolean is_active
    }

    RESOURCE_UNITS {
        bigint id PK
        bigint resource_id FK
        varchar asset_code UK
        varchar serial_number
        varchar unit_status
        varchar physical_condition
    }

    RESERVATIONS {
        bigint id PK
        bigint user_id FK
        bigint resource_id FK
        varchar status
        int quantity
        date start_date
        date end_date
        datetime claim_deadline
    }

    BORROWING_RECORDS {
        bigint id PK
        bigint user_id FK
        bigint resource_id FK
        bigint resource_unit_id FK
        varchar status
        int issued_quantity
        int returned_quantity
        date borrow_date
        date due_date
    }

    RETURN_TRANSACTIONS {
        bigint id PK
        bigint borrowing_record_id FK
        bigint processed_by FK
        int quantity_returned
        varchar return_condition
        datetime return_timestamp
    }
```

---

## 4. Data Flow Diagram – Level 0 (Context Level)

```mermaid
flowchart LR
    StudentUser["Student / Borrower"]
    FacultyUser["Faculty Reviewer"]
    DeskStaff["Circulation Staff / Admin"]
    SystemEngine["BorrowHub Platform (APPJFS12)"]
    MySQLDB[("Institutional Database (MySQL 8.0)")]

    StudentUser -->|"Credentials, Reservations, Borrow Requests"| SystemEngine
    SystemEngine -->|"Auth Tokens, Loan Status, Availability Alerts"| StudentUser

    FacultyUser -->|"Approval / Rejection Decisions"| SystemEngine
    SystemEngine -->|"Pending Review Queue"| FacultyUser

    DeskStaff -->|"Checkout (Asset Scan), Return Intake, Condition Logs"| SystemEngine
    SystemEngine -->|"Circulation Records, Overdue Reports, Inventory Status"| DeskStaff

    SystemEngine -->|"ACID State Changes, Level 0-6 DAG Locks, Flyway Schema"| MySQLDB
    MySQLDB -->|"Entity States, Active Counts, Fenced Leases"| SystemEngine
```

---

## 5. Data Flow Diagram – Level 1

```mermaid
flowchart TD
    subgraph DFD1["Level 1 System Processes"]
        P1["1.0 User Authentication & Token Issuance"]
        P2["2.0 Catalog Discovery & Availability Check"]
        P3["3.0 Reservation Management & Unit Hold"]
        P4["4.0 Borrowing Request Workflow & Review"]
        P5["5.0 Circulation Desk (Checkout / Return)"]
        P6["6.0 Outbox Event Dispatch & Notification"]
    end

    subgraph DataStores["System Data Stores"]
        D1[("D1: Users & Roles")]
        D2[("D2: Resources & Inventory Units")]
        D3[("D3: Reservations & Allotments")]
        D4[("D4: Borrowing Records & Returns")]
        D5[("D5: Outbox Events & Audit Logs")]
    end

    User(["User (Student/Faculty/Admin)"]) -->|"Login Credentials"| P1
    P1 -->|"Query User Hash"| D1
    P1 -->|"Issue Signed JWT"| User

    User -->|"Search Filter Query"| P2
    P2 -->|"Read Stock & Mode"| D2
    P2 -->|"Return Catalog Page"| User

    User -->|"Submit Reserve Request"| P3
    P3 -->|"Lock Stock & Allocate Units"| D2
    P3 -->|"Create Reservation Record"| D3
    P3 -->|"Queue Stock Hold Event"| D5

    User -->|"Submit Borrow Request"| P4
    P4 -->|"Store Pending Request"| D3
    P4 -->|"Faculty Approval Updates Status"| D3

    User -->|"Present Claim at Desk"| P5
    P5 -->|"Fenced Idempotency Check"| D5
    P5 -->|"Update Reservation to FULFILLED"| D3
    P5 -->|"Create Borrowing Record (ISSUED)"| D4
    P5 -->|"Transition Unit to BORROWED"| D2

    P6 -->|"JIT Lease Claim"| D5
    P6 -->|"Dispatch Notifications"| User
```

---

## 6. Sequence Diagram: Resource Reservation

```mermaid
sequenceDiagram
    autonumber
    actor Student as Student Borrower
    participant Web as React SPA Frontend
    participant Ctrl as ReservationController
    participant Svc as ReservationService
    participant Avail as AvailabilityService
    participant Repo as ReservationRepository
    participant DB as MySQL 8.0 InnoDB

    Student->>Web: Select Resource & Dates (Qty: 2)
    Web->>Ctrl: POST /api/reservations (Bearer JWT)
    Ctrl->>Svc: createReservation(userId, dto)
    Svc->>Avail: checkAvailability(resourceId, dates, qty)
    Avail->>DB: SELECT available_quantity FOR UPDATE
    DB-->>Avail: Available Quantity Confirmed
    Avail-->>Svc: Availability Guaranteed
    Svc->>Repo: save(Reservation[Status=HELD, Deadline=NOW()+24H])
    Repo->>DB: INSERT INTO reservations (...)
    Svc->>DB: INSERT INTO reservation_units (unit_ids...)
    Svc->>DB: INSERT INTO outbox_events (RESERVATION_HELD)
    DB-->>Svc: Commit Transaction
    Svc-->>Ctrl: ReservationResponseDto
    Ctrl-->>Web: 201 Created (ApiResponse)
    Web-->>Student: Display Reservation Confirmation with Claim Deadline
```

---

## 7. Sequence Diagram: Borrowing Approval

```mermaid
sequenceDiagram
    autonumber
    actor Faculty as Faculty Reviewer
    participant Web as React SPA Frontend
    participant Ctrl as BorrowingController
    participant Svc as BorrowingRequestService
    participant Outbox as OutboxService
    participant DB as MySQL 8.0 InnoDB

    Faculty->>Web: Open Pending Approvals Dashboard
    Web->>Ctrl: GET /api/borrowings/requests/pending
    Ctrl->>DB: SELECT * FROM borrowing_requests WHERE status='PENDING_REVIEW'
    DB-->>Ctrl: List of Requests
    Ctrl-->>Web: 200 OK (Requests List)
    Faculty->>Web: Click "Approve Request" (ID: 101, Comments: "Approved for Lab")
    Web->>Ctrl: POST /api/borrowings/requests/101/review
    Ctrl->>Svc: reviewRequest(101, facultyId, APPROVED)
    Svc->>DB: SELECT * FROM borrowing_requests WHERE id=101 FOR UPDATE
    Svc->>DB: UPDATE borrowing_requests SET status='APPROVED', reviewer_id=facultyId
    Svc->>Outbox: publishEvent("BORROWING_APPROVED", requestId)
    Outbox->>DB: INSERT INTO outbox_events (...)
    DB-->>Svc: Commit Transaction
    Svc-->>Ctrl: BorrowingRequestResponseDto
    Ctrl-->>Web: 200 OK (Approval Recorded)
    Web-->>Faculty: Badge Updated to "APPROVED"
```

---

## 8. Sequence Diagram: Circulation Checkout and Return

```mermaid
sequenceDiagram
    autonumber
    actor Staff as Circulation Staff
    participant Web as React SPA Frontend
    participant Ctrl as CirculationController
    participant Orch as CirculationRetryOrchestrator
    participant TxSvc as CirculationTransactionalService
    participant Idem as IdempotencyService
    participant DB as MySQL 8.0 InnoDB

    Note over Staff,DB: Phase A: Equipment Checkout
    Staff->>Web: Scan Asset Tag & Enter Reservation ID
    Web->>Ctrl: POST /api/circulation/checkout (Idempotency-Key: UUID)
    Ctrl->>Orch: checkout(dto, staffId, key)
    Orch->>Idem: checkOrAcquireLease(key, payloadHash)
    Idem->>DB: INSERT INTO idempotency_records (...)
    Orch->>TxSvc: executeCheckoutTransaction(...)
    TxSvc->>DB: Lock Resource Unit & Reservation (Level 2 & 3)
    TxSvc->>DB: UPDATE reservations SET status='FULFILLED'
    TxSvc->>DB: UPDATE resource_units SET unit_status='BORROWED'
    TxSvc->>DB: INSERT INTO borrowing_records (status='ACTIVE')
    TxSvc->>DB: Record Level-6 Outbox Event (EQUIPMENT_CHECKED_OUT)
    TxSvc-->>Orch: Checkout Completed
    Orch->>Idem: markCompleted(key, responseJson)
    Orch-->>Ctrl: CheckoutResponseDto
    Ctrl-->>Web: 200 OK (Equipment Issued)

    Note over Staff,DB: Phase B: Return Intake & Inspection
    Staff->>Web: Enter Return Intake (Qty: 2, Condition: GOOD)
    Web->>Ctrl: POST /api/circulation/return (Idempotency-Key: UUID)
    Ctrl->>Orch: processReturn(dto, staffId, key)
    Orch->>TxSvc: executeReturnTransaction(...)
    TxSvc->>DB: Lock BorrowingRecord FOR UPDATE
    TxSvc->>DB: UPDATE borrowing_records SET returned_quantity=2, status='RETURNED'
    TxSvc->>DB: UPDATE resource_units SET unit_status='AVAILABLE'
    TxSvc->>DB: INSERT INTO return_transactions (...)
    TxSvc->>DB: INSERT INTO outbox_events (STOCK_REPLENISHED)
    TxSvc-->>Orch: Return Processed
    Orch-->>Ctrl: ReturnResponseDto
    Ctrl-->>Web: 200 OK (Return Recorded & Inventory Restored)
```

---

## 9. Activity Diagram: Resource Borrowing Lifecycle

```mermaid
flowchart TD
    Start(["Student Initiates Resource Request"]) --> SearchCatalog["Search & Browse Catalog"]
    SearchCatalog --> CheckMode{"Inventory Mode?"}

    CheckMode -->|"BULK / CONSUMABLE"| DirectReserve["Reserve Bulk Quantity"]
    CheckMode -->|"INDIVIDUAL / SERIALIZED"| SelectUnit["Select Specific Model / Asset"]

    DirectReserve --> StockAvail{"Stock Available?"}
    SelectUnit --> StockAvail

    StockAvail -->|"No"| JoinWaitlist["Join Waitlist Queue"]
    JoinWaitlist --> WaitNotification["Wait for Stock Replenishment Notification"]
    WaitNotification --> EndCancelled(["Candidate Notified When Stock Returns"])

    StockAvail -->|"Yes"| HoldReservation["Hold Reservation (Status: HELD, Claim Deadline: +24h)"]
    HoldReservation --> NeedsApproval{"Requires Faculty Approval?"}

    NeedsApproval -->|"Yes"| SubmitApproval["Submit Borrowing Request (PENDING_REVIEW)"]
    SubmitApproval --> FacultyDecision{"Faculty Decision"}
    FacultyDecision -->|"Rejected"| CancelReserve["Release Held Stock -> Notify Waitlist"]
    CancelReserve --> EndCancelled
    FacultyDecision -->|"Approved"| ReadyForPickup["Reservation Status: APPROVED & HELD"]

    NeedsApproval -->|"No"| ReadyForPickup

    ReadyForPickup --> CheckDeadline{"Claimed Before Deadline?"}
    CheckDeadline -->|"Deadline Elapsed"| ExpireReservation["Durable Expiry Sweeper Marks EXPIRED"]
    ExpireReservation --> RestockRelease["Stock Restored & Outbox Emitted"]
    RestockRelease --> EndCancelled

    CheckDeadline -->|"Arrives at Desk"| PhysicalCheckout["Staff Verifies ID & Scans QR/Barcode"]
    PhysicalCheckout --> FencedCheckout["Atomic Level-0 Fenced Checkout Transaction"]
    FencedCheckout --> ActiveLoan["Borrowing Record Status: ACTIVE / ISSUED"]

    ActiveLoan --> UsagePeriod["Student Uses Equipment"]
    UsagePeriod --> CheckDueDate{"Due Date Exceeded?"}
    CheckDueDate -->|"Overdue"| MidnightSweep["Overdue Sweeper Sets Status: OVERDUE"]
    MidnightSweep --> IntakeDesk["Student Returns Item to Desk"]
    CheckDueDate -->|"On Time"| IntakeDesk

    IntakeDesk --> InspectCondition["Staff Performs Physical Condition Inspection"]
    InspectCondition --> HasDamage{"Damage / Loss Detected?"}

    HasDamage -->|"Yes"| LogMaintenance["Log Condition & Route Unit to MAINTENANCE / DAMAGED"]
    HasDamage -->|"No"| UnitAvailable["Unit Status: AVAILABLE"]

    LogMaintenance --> FinalReturnRecord["Record Return Transaction & Fulfill Loan"]
    UnitAvailable --> FinalReturnRecord

    FinalReturnRecord --> StockReplenished["Emit STOCK_REPLENISHED Outbox Event"]
    StockReplenished --> EndSuccess(["Loan Cycle Complete & Consistent"])
```
