# BorrowHub Database Backup & Recovery Guide

**System:** BorrowHub Inventory & Resource Management Platform  
**Target Engine:** MySQL 8.0.43 InnoDB (ACID Compliant)  
**Timezone Standard:** `+05:30` (`Asia/Kolkata`)  
**Character Set / Collation:** `utf8mb4` / `utf8mb4_unicode_ci`  

---

## 1. Overview & Strategy

BorrowHub enforces database-authoritative state transitions, strict multi-version concurrency control (MVCC), Level 0–6 transaction locking hierarchies, and Flyway schema migrations.

To protect against data corruption, hardware failure, accidental deletion, or disaster scenarios, the platform mandates regular **Logical Backups** via `mysqldump` and **Binary Logging** for point-in-time recovery (PITR).

---

## 2. Backup Methods

### A. Automated One-Click Backup Script
BorrowHub provides a dedicated Windows batch utility in `scripts/backup-database.bat`.

```cmd
scripts\backup-database.bat
```

**Features:**
- Loads credentials securely from the project's root `.env` file.
- Never prints or leaks database passwords to standard output.
- Exports to `database/backups/borrowhub_backup_YYYYMMDD_HHMMSS.sql`.
- Employs `--single-transaction` to eliminate table locks during backup.
- Includes routines, triggers, and events.

### B. Manual CLI Backup Command

```bash
mysqldump \
  --host=127.0.0.1 \
  --port=3306 \
  --user=borrowhub_user \
  --single-transaction \
  --quick \
  --routines \
  --triggers \
  --hex-blob \
  --default-character-set=utf8mb4 \
  borrowhub_db > database/backups/borrowhub_manual_backup.sql
```

> **Important Parameters:**
> - `--single-transaction`: Uses InnoDB snapshot isolation so backup is consistent without locking tables.
> - `--quick`: Streams rows directly to disk instead of buffering the entire table in RAM.
> - `--hex-blob`: Escapes binary fields safely to prevent syntax errors during restoration.
> - `--default-character-set=utf8mb4`: Preserves full Unicode compliance.

---

## 3. Database Restoration Procedure

### Step 1: Ensure Target Database Exists
Connect to MySQL as administrative user or `borrowhub_user` and ensure the database exists:

```sql
CREATE DATABASE IF NOT EXISTS borrowhub_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

### Step 2: Stop BorrowHub Application
Before restoring data, stop backend and background schedulers to avoid concurrent writes:

```cmd
scripts\stop-borrowhub.bat
```

### Step 3: Execute Restoration
Run the MySQL client importing the backup SQL file:

```cmd
mysql -h 127.0.0.1 -P 3306 -u borrowhub_user -p borrowhub_db < database/backups/borrowhub_backup_20261113_131500.sql
```

### Step 4: Verify Flyway Schema State
Check that the schema history table is aligned:

```sql
SELECT installed_rank, version, description, type, script, success 
FROM borrowhub_db.flyway_schema_history 
ORDER BY installed_rank DESC LIMIT 5;
```

### Step 5: Restart BorrowHub Application
```cmd
scripts\start-borrowhub.bat
```

---

## 4. Disaster Recovery & Integrity Verification

Following any database restoration, run the BorrowHub automated QA verification suites:

1. **Verify Database Status:**
   ```cmd
   scripts\database-status.bat
   ```
2. **Execute Application Health Check:**
   ```cmd
   scripts\health-check.bat
   ```
3. **Run Comprehensive Functional State Machine Verification:**
   ```powershell
   powershell -NoProfile -ExecutionPolicy Bypass -File qa/integration/test-all-flows.ps1
   ```
