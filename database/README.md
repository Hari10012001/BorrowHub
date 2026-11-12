# BorrowHub — Database Architecture & Migrations

## Overview
BorrowHub utilizes **MySQL 8.0.43 InnoDB** as its primary persistent datastore and authoritative clock arbiter (`Asia/Kolkata` `+05:30`). The database schema is fully managed via Flyway migrations located in `borrowhub-backend/src/main/resources/db/migration/`.

## Key Schema Characteristics
1. **17 Relational Tables:**
   - User Management: `users`
   - Catalog: `categories`, `resources`, `resource_units`
   - Requests & Holds: `borrowing_requests`, `reservations`, `reservation_units`
   - Circulation: `borrowing_records`, `borrowing_record_units`, `return_inspections`, `penalties`
   - Queue: `waitlist_entries`
   - Reliability & Concurrency: `idempotency_keys`, `outbox_events`
   - Governance: `system_settings`, `audit_logs`, `notification_logs`
2. **STORED Generated Columns (C17):**
   - `quantity_outstanding` in `borrowing_records` is evaluated by MySQL as `(quantity_issued - quantity_returned - quantity_lost)` and indexed.
3. **Check Constraints (C13):**
   - `chk_br_terminal_consistency`: Enforces terminal state consistency on return and loss.
   - `chk_br_individual_status`: Enforces individual physical unit atomic state transitions.
4. **DAG Locking Foreign Key Indices (C43):**
   - All foreign keys are indexed to prevent gap locks and lock escalation during concurrent joins.

## Operational Commands
Use the scripts in the project root `scripts/` directory:
- `scripts\start-database.bat` — Starts the configured MySQL 8.0 server.
- `scripts\stop-database.bat` — Gracefully stops the server.
- `scripts\database-status.bat` — Inspects service status, version, connection, and table count.
- `scripts\open-database.bat` — Launches an interactive MySQL console session.

## Test Data & Backups
- `database/test-data/` — Isolated seed datasets for smoke and integration tests.
- `database/backups/` — Target directory for automated logical database dumps.
- `database/scripts/` — Auxiliary database maintenance and health scripts.
