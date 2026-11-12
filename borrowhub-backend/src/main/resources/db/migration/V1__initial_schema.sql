-- ============================================================================
-- BORROWHUB DATABASE SPECIFICATION — V1 INITIAL SCHEMA (DAG DEPENDENCY ORDER)
-- Target DBMS: MySQL 8.0+ (InnoDB Engine)
-- Version: 2.4.8 — Production Hardening
-- ============================================================================

-- LEVEL 0: Independent Root Tables (No Foreign Keys)
CREATE TABLE users (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    username            VARCHAR(50)  NOT NULL UNIQUE,
    email               VARCHAR(100) NOT NULL UNIQUE,
    password_hash       VARCHAR(255) NOT NULL,
    first_name          VARCHAR(50)  NOT NULL,
    last_name           VARCHAR(50)  NOT NULL,
    phone               VARCHAR(20),
    department          VARCHAR(100) NOT NULL,
    institutional_id    VARCHAR(50)  NOT NULL UNIQUE COMMENT 'Student Roll Number or Faculty ID',
    role                ENUM('ADMIN', 'FACULTY', 'STUDENT') NOT NULL DEFAULT 'STUDENT',
    account_status      ENUM('ACTIVE', 'SUSPENDED', 'DEACTIVATED') NOT NULL DEFAULT 'ACTIVE',
    profile_image_url   VARCHAR(255),
    created_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    INDEX idx_users_role (role),
    INDEX idx_users_status (account_status),
    INDEX idx_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE categories (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                VARCHAR(100) NOT NULL UNIQUE,
    slug                VARCHAR(100) NOT NULL UNIQUE,
    description         TEXT,
    icon_name           VARCHAR(50)  DEFAULT 'Folder',
    image_url           VARCHAR(255),
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    display_order       INT NOT NULL DEFAULT 0,
    created_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    INDEX idx_categories_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE audit_logs (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_user_id       BIGINT NULL,
    actor_username      VARCHAR(50) NOT NULL,
    action              VARCHAR(100) NOT NULL,
    entity_name         VARCHAR(50) NOT NULL,
    entity_id           BIGINT NULL,
    ip_address          VARCHAR(45),
    details             JSON,
    created_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    INDEX idx_audit_actor (actor_user_id),
    INDEX idx_audit_action (action),
    INDEX idx_audit_entity (entity_name, entity_id),
    INDEX idx_audit_timestamp (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE idempotency_records (
    idempotency_key     VARCHAR(64) PRIMARY KEY,
    user_id             BIGINT NOT NULL COMMENT 'Authenticated user extracted from security context (C23)',
    endpoint            VARCHAR(100) NOT NULL COMMENT 'Target endpoint URI preventing cross-endpoint reuse (C23)',
    request_hash        VARCHAR(64) NOT NULL COMMENT 'SHA-256 of endpoint:userId:rfc8785CanonicalJson(body) (C26/C41/C45)',
    response_status     INT NULL,
    response_body       JSON NULL,
    status              ENUM('IN_PROGRESS', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'IN_PROGRESS',
    failure_type        ENUM('TERMINAL', 'RETRYABLE') NULL COMMENT 'Differentiates replayable business errors from retryable transient errors (C25)',
    owner_token         VARCHAR(64) NULL COMMENT 'Worker node UUID holding lease for fencing (C24/C27)',
    fence_generation    BIGINT NOT NULL DEFAULT 0 COMMENT 'Monotonically increasing token to fence off zombie workers (C24/C27)',
    lease_expires_at    TIMESTAMP(6) NOT NULL COMMENT 'Authoritative lease expiration deadline evaluated via NOW(6) (C34/C40)',
    created_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    INDEX idx_idempotency_user (user_id),
    INDEX idx_idempotency_endpoint (endpoint),
    INDEX idx_idempotency_lease (status, lease_expires_at),
    INDEX idx_idempotency_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE outbox_events (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type          VARCHAR(100) NOT NULL COMMENT 'Canonical: STOCK_REPLENISHED, RESERVATION_EXPIRED, BORROWING_OVERDUE, WAITLIST_PROMOTED (C39)',
    aggregate_id        BIGINT NOT NULL COMMENT 'Target entity ID (resource_id or reservation_id)',
    payload             JSON NOT NULL,
    status              ENUM('PENDING', 'IN_FLIGHT', 'PROCESSED', 'DEAD_LETTER') NOT NULL DEFAULT 'PENDING',
    retry_count         INT NOT NULL DEFAULT 0,
    claimed_by          VARCHAR(100) NULL COMMENT 'Worker node identifier holding processing lease (C28)',
    claimed_at          TIMESTAMP(6) NULL COMMENT 'Timestamp when worker acquired lease (C28)',
    lease_expires_at    TIMESTAMP(6) NULL COMMENT 'Authoritative lease deadline evaluated via NOW(6) (C33/C37/C40/C44/C48/C49)',
    fence_generation    BIGINT NOT NULL DEFAULT 0 COMMENT 'Monotonically increasing token to fence stale outbox workers (C28/C33/C37/C48)',
    next_retry_at       TIMESTAMP(6) NULL COMMENT 'Authoritative backoff schedule evaluated via NOW(6) (C29/C34/C40)',
    last_error          TEXT NULL COMMENT 'Diagnostic stack trace for operational visibility (C31)',
    created_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    processed_at        TIMESTAMP(6) NULL,
    updated_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    INDEX idx_outbox_claim (status, next_retry_at, lease_expires_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE processed_events (
    event_id            BIGINT NOT NULL,
    consumer_name       VARCHAR(100) NOT NULL COMMENT 'Consumer component or handler identifier',
    processed_at        TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (event_id, consumer_name),
    CONSTRAINT fk_pe_event FOREIGN KEY (event_id) REFERENCES outbox_events (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- LEVEL 1: Tables depending on Level 0
CREATE TABLE resources (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id         BIGINT NOT NULL,
    resource_code       VARCHAR(50)  NOT NULL UNIQUE,
    name                VARCHAR(150) NOT NULL,
    description         TEXT,
    image_url           VARCHAR(255),
    inventory_mode      ENUM('QUANTITY', 'INDIVIDUAL') NOT NULL DEFAULT 'INDIVIDUAL',
    total_quantity      INT NOT NULL DEFAULT 1,
    max_borrow_days     INT NOT NULL DEFAULT 7,
    is_reservable       BOOLEAN NOT NULL DEFAULT TRUE,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    version             BIGINT NOT NULL DEFAULT 0,
    created_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_res_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE RESTRICT,
    CONSTRAINT chk_res_total_qty CHECK (total_quantity >= 0),
    CONSTRAINT chk_res_borrow_days CHECK (max_borrow_days > 0),
    INDEX idx_res_category (category_id),
    INDEX idx_res_mode (inventory_mode),
    INDEX idx_res_active (is_active),
    FULLTEXT INDEX idx_res_search (name, description)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notifications (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    title               VARCHAR(150) NOT NULL,
    message             TEXT NOT NULL,
    type                ENUM('REQUEST_UPDATE', 'RESERVATION_CONFIRMED', 'DUE_SOON', 'OVERDUE_ALERT', 'WAITLIST_AVAILABLE', 'MAINTENANCE_ALERT', 'GENERAL') NOT NULL,
    reference_type      VARCHAR(50),
    reference_id        BIGINT,
    is_read             BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_notif_user_read (user_id, is_read, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- LEVEL 2: Tables depending on Resources and Users
CREATE TABLE resource_units (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    resource_id         BIGINT NOT NULL,
    asset_code          VARCHAR(50)  NOT NULL UNIQUE,
    qr_code_data        VARCHAR(255) NOT NULL UNIQUE,
    serial_number       VARCHAR(100),
    brand_model         VARCHAR(150),
    physical_condition  ENUM('NEW', 'GOOD', 'FAIR', 'POOR', 'DAMAGED') NOT NULL DEFAULT 'GOOD',
    status              ENUM('AVAILABLE', 'BORROWED', 'MAINTENANCE', 'RETIRED', 'LOST') NOT NULL DEFAULT 'AVAILABLE',
    storage_location    VARCHAR(100) NOT NULL,
    acquisition_date    DATE,
    notes               TEXT,
    created_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_unit_resource FOREIGN KEY (resource_id) REFERENCES resources (id) ON DELETE RESTRICT,
    INDEX idx_unit_resource_status (resource_id, status),
    INDEX idx_unit_asset_code (asset_code),
    INDEX idx_unit_qr_data (qr_code_data)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE borrowing_requests (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_number      VARCHAR(50)  NOT NULL UNIQUE,
    user_id             BIGINT NOT NULL,
    resource_id         BIGINT NOT NULL,
    quantity            INT NOT NULL DEFAULT 1,
    requested_start     DATE NOT NULL,
    requested_end       DATE NOT NULL,
    purpose             TEXT NOT NULL,
    status              ENUM('PENDING_REVIEW', 'APPROVED', 'REJECTED', 'CANCELLED', 'EXPIRED', 'FULFILLED') NOT NULL DEFAULT 'PENDING_REVIEW',
    rejection_reason    TEXT,
    reviewed_by         BIGINT,
    reviewed_at         TIMESTAMP(6) NULL,
    created_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_req_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_req_resource FOREIGN KEY (resource_id) REFERENCES resources (id) ON DELETE RESTRICT,
    CONSTRAINT fk_req_reviewer FOREIGN KEY (reviewed_by) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_req_dates CHECK (requested_end >= requested_start),
    CONSTRAINT chk_req_qty CHECK (quantity > 0),
    INDEX idx_req_user (user_id),
    INDEX idx_req_resource (resource_id),
    INDEX idx_req_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE waitlist_entries (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    resource_id         BIGINT NOT NULL,
    user_id             BIGINT NOT NULL,
    priority_tier       INT NOT NULL DEFAULT 2 COMMENT '1 = Faculty, 2 = Student',
    queue_position      INT NOT NULL,
    requested_start     DATE NOT NULL,
    requested_end       DATE NOT NULL,
    status              ENUM('WAITING', 'NOTIFIED', 'CLAIMED', 'EXPIRED', 'CANCELLED') NOT NULL DEFAULT 'WAITING',
    notified_at         TIMESTAMP(6) NULL,
    claim_deadline      TIMESTAMP(6) NULL,
    created_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_wl_resource FOREIGN KEY (resource_id) REFERENCES resources (id) ON DELETE CASCADE,
    CONSTRAINT fk_wl_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_wl_dates CHECK (requested_end >= requested_start),
    INDEX idx_wl_queue (resource_id, status, priority_tier, queue_position)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- LEVEL 3: Tables depending on Level 2
CREATE TABLE reservations (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_number  VARCHAR(50)  NOT NULL UNIQUE,
    request_id          BIGINT NOT NULL UNIQUE,
    user_id             BIGINT NOT NULL,
    resource_id         BIGINT NOT NULL,
    quantity            INT NOT NULL DEFAULT 1,
    start_date          DATE NOT NULL,
    end_date            DATE NOT NULL,
    claim_deadline      TIMESTAMP(6) NOT NULL COMMENT 'Authoritative deadline evaluated strictly via NOW(6) (C34/C40)',
    status              ENUM('HELD', 'CLAIMED', 'CANCELLED', 'EXPIRED') NOT NULL DEFAULT 'HELD',
    cancellation_reason TEXT,
    created_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_res_req FOREIGN KEY (request_id) REFERENCES borrowing_requests (id) ON DELETE RESTRICT,
    CONSTRAINT fk_res_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_res_resource FOREIGN KEY (resource_id) REFERENCES resources (id) ON DELETE RESTRICT,
    CONSTRAINT chk_res_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_res_qty CHECK (quantity > 0),
    INDEX idx_res_resource_dates (resource_id, start_date, end_date, status),
    INDEX idx_res_user_status (user_id, status),
    INDEX idx_res_claim_deadline (claim_deadline, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE maintenance_records (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    resource_unit_id    BIGINT NOT NULL,
    maintenance_type    ENUM('PREVENTIVE', 'CORRECTIVE', 'CALIBRATION', 'REPAIR') NOT NULL DEFAULT 'PREVENTIVE',
    description         TEXT NOT NULL,
    status              ENUM('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'SCHEDULED',
    cost                DECIMAL(10,2) DEFAULT 0.00,
    service_provider    VARCHAR(150),
    scheduled_date      DATE NOT NULL,
    completion_date     DATE NULL,
    resolution_notes    TEXT,
    initiated_by        BIGINT NOT NULL,
    created_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_maint_unit FOREIGN KEY (resource_unit_id) REFERENCES resource_units (id) ON DELETE RESTRICT,
    CONSTRAINT fk_maint_user FOREIGN KEY (initiated_by) REFERENCES users (id) ON DELETE RESTRICT,
    INDEX idx_maint_unit_status (resource_unit_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- LEVEL 4: Tables depending on Reservations & Resource Units
CREATE TABLE reservation_units (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_id      BIGINT NOT NULL,
    resource_unit_id    BIGINT NOT NULL,
    allocated_from      DATE NOT NULL,
    allocated_until     DATE NOT NULL,
    created_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_ru_reservation FOREIGN KEY (reservation_id) REFERENCES reservations (id) ON DELETE CASCADE,
    CONSTRAINT fk_ru_unit FOREIGN KEY (resource_unit_id) REFERENCES resource_units (id) ON DELETE RESTRICT,
    CONSTRAINT uq_reservation_unit_link UNIQUE (reservation_id, resource_unit_id),
    CONSTRAINT chk_ru_dates CHECK (allocated_until >= allocated_from),
    INDEX idx_ru_unit_dates (resource_unit_id, allocated_from, allocated_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE borrowing_records (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_code    VARCHAR(50)  NOT NULL UNIQUE,
    idempotency_key     VARCHAR(64)  NULL UNIQUE COMMENT 'C27: Durable idempotency link preventing duplicate loans',
    reservation_id      BIGINT NOT NULL,
    user_id             BIGINT NOT NULL,
    resource_id         BIGINT NOT NULL,
    resource_unit_id    BIGINT NULL COMMENT 'NULL for QUANTITY mode items',
    quantity_issued     INT NOT NULL DEFAULT 1,
    quantity_returned   INT NOT NULL DEFAULT 0 COMMENT 'Cumulative units physically received (accepted + damaged). Excludes lost.',
    quantity_damaged    INT NOT NULL DEFAULT 0 COMMENT 'Subset of quantity_returned that were damaged on receipt.',
    quantity_lost       INT NOT NULL DEFAULT 0 COMMENT 'Declared lost via loss declaration endpoint. Never physically received.',
    quantity_outstanding INT GENERATED ALWAYS AS (quantity_issued - quantity_returned - quantity_lost) STORED
                        COMMENT 'Authoritative remaining custody balance.',
    issue_date          DATE NOT NULL DEFAULT (CURRENT_DATE) COMMENT 'C42: Generated strictly by MySQL engine',
    due_date            DATE NOT NULL,
    actual_return_date  DATE NULL COMMENT 'C40: Set via MySQL CURRENT_DATE() on return',
    closed_at           TIMESTAMP(6) NULL COMMENT 'C40: Set via MySQL NOW(6) on closure',
    condition_at_issue  ENUM('NEW', 'GOOD', 'FAIR', 'POOR') NOT NULL DEFAULT 'GOOD',
    condition_at_return ENUM('NEW', 'GOOD', 'FAIR', 'POOR', 'DAMAGED', 'LOST') NULL,
    issued_by           BIGINT NOT NULL,
    returned_to         BIGINT NULL,
    previous_status     ENUM('ACTIVE', 'PARTIALLY_RETURNED', 'OVERDUE') NULL
                        COMMENT 'Set to current status before entering RETURN_PENDING. Restored on rejection. NULL otherwise.',
    status              ENUM('ACTIVE', 'PARTIALLY_RETURNED', 'RETURN_PENDING', 'RETURNED', 'OVERDUE', 'LOST', 'CLOSED') NOT NULL DEFAULT 'ACTIVE',
    remarks             TEXT,
    created_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_br_reservation FOREIGN KEY (reservation_id) REFERENCES reservations (id) ON DELETE RESTRICT,
    CONSTRAINT fk_br_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_br_resource FOREIGN KEY (resource_id) REFERENCES resources (id) ON DELETE RESTRICT,
    CONSTRAINT fk_br_unit FOREIGN KEY (resource_unit_id) REFERENCES resource_units (id) ON DELETE RESTRICT,
    CONSTRAINT fk_br_issuer FOREIGN KEY (issued_by) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_br_returnee FOREIGN KEY (returned_to) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_br_dates CHECK (due_date >= issue_date),
    CONSTRAINT chk_br_issued_qty CHECK (quantity_issued > 0),
    CONSTRAINT chk_br_accounting CHECK (
        quantity_returned >= 0 AND
        quantity_damaged >= 0 AND
        quantity_lost >= 0 AND
        quantity_damaged <= quantity_returned AND
        (quantity_returned + quantity_lost) <= quantity_issued
    ),
    CONSTRAINT chk_br_individual_status CHECK (
        resource_unit_id IS NULL OR (
            quantity_issued = 1
            AND status NOT IN ('PARTIALLY_RETURNED', 'CLOSED')
        )
    ),
    CONSTRAINT chk_br_prev_status CHECK (
        (status = 'RETURN_PENDING' AND previous_status IS NOT NULL)
        OR (status != 'RETURN_PENDING' AND previous_status IS NULL)
    ),
    CONSTRAINT chk_br_terminal_consistency CHECK (
        -- 1. Pure physical return:
        (status = 'RETURNED' AND quantity_outstanding = 0 AND quantity_lost = 0 AND quantity_returned = quantity_issued AND actual_return_date IS NOT NULL AND closed_at IS NULL)
        -- 2. Pure loss declaration (bulk quantity):
        OR (status = 'CLOSED' AND quantity_outstanding = 0 AND quantity_returned = 0 AND quantity_lost = quantity_issued AND actual_return_date IS NULL AND closed_at IS NOT NULL)
        -- 3. Mixed return and loss outcome (bulk quantity):
        OR (status = 'CLOSED' AND quantity_outstanding = 0 AND quantity_returned > 0 AND quantity_lost > 0 AND (quantity_returned + quantity_lost = quantity_issued) AND actual_return_date IS NOT NULL AND closed_at IS NOT NULL)
        -- 4. Individual asset lost:
        OR (status = 'LOST' AND quantity_issued = 1 AND quantity_returned = 0 AND quantity_lost = 1 AND quantity_outstanding = 0 AND actual_return_date IS NULL AND closed_at IS NOT NULL AND resource_unit_id IS NOT NULL)
        -- 5. Non-terminal in-flight states:
        OR (status IN ('ACTIVE', 'PARTIALLY_RETURNED', 'RETURN_PENDING', 'OVERDUE') AND quantity_outstanding > 0 AND closed_at IS NULL)
    ),
    INDEX idx_br_user_status (user_id, status),
    INDEX idx_br_resource_status (resource_id, status),
    INDEX idx_br_unit_status (resource_unit_id, status),
    INDEX idx_br_overdue (due_date, status),
    INDEX idx_br_outstanding (resource_id, quantity_outstanding, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- LEVEL 5: Tables depending on Borrowing Records
CREATE TABLE return_transactions (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    borrowing_record_id BIGINT NOT NULL,
    returned_quantity   INT NOT NULL COMMENT 'Physical units received in this transaction (accepted + damaged).',
    damaged_quantity    INT NOT NULL DEFAULT 0 COMMENT 'Units received damaged in this transaction.',
    accepted_quantity   INT NOT NULL COMMENT 'Undamaged units received in this transaction; restored to shelf stock.',
    condition_assessed  ENUM('NEW', 'GOOD', 'FAIR', 'POOR', 'DAMAGED') NOT NULL,
    received_by         BIGINT NOT NULL,
    return_notes        TEXT,
    returned_at         TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_rt_record FOREIGN KEY (borrowing_record_id) REFERENCES borrowing_records (id) ON DELETE RESTRICT,
    CONSTRAINT fk_rt_receiver FOREIGN KEY (received_by) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_rt_quantities CHECK (
        returned_quantity > 0 AND
        accepted_quantity >= 0 AND
        damaged_quantity >= 0 AND
        (accepted_quantity + damaged_quantity) = returned_quantity
    ),
    INDEX idx_rt_record (borrowing_record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE condition_logs (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    resource_unit_id    BIGINT NOT NULL,
    borrowing_record_id BIGINT NULL,
    logged_by           BIGINT NOT NULL,
    previous_condition  ENUM('NEW', 'GOOD', 'FAIR', 'POOR', 'DAMAGED') NOT NULL,
    new_condition       ENUM('NEW', 'GOOD', 'FAIR', 'POOR', 'DAMAGED') NOT NULL,
    inspection_notes    TEXT NOT NULL,
    created_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_cl_unit FOREIGN KEY (resource_unit_id) REFERENCES resource_units (id) ON DELETE RESTRICT,
    CONSTRAINT fk_cl_record FOREIGN KEY (borrowing_record_id) REFERENCES borrowing_records (id) ON DELETE SET NULL,
    CONSTRAINT fk_cl_inspector FOREIGN KEY (logged_by) REFERENCES users (id) ON DELETE RESTRICT,
    INDEX idx_cl_unit (resource_unit_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
