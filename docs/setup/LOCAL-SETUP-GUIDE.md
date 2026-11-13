# BorrowHub — Local Development Setup Guide

This guide provides end-to-end instructions for configuring, running, and developing the **BorrowHub** platform on a Windows local machine.

---

## 1. Prerequisites

Ensure the following runtimes and toolchains are installed on your workstation:

| Component | Minimum Version | Verification Command | Notes |
|---|---|---|---|
| **Java JDK** | 21 LTS | `java -version` | Oracle OpenJDK or Eclipse Temurin 21 |
| **Apache Maven** | 3.9+ | `mvn -version` | Build tool for Spring Boot |
| **Node.js** | 18+ LTS (20+ recommended) | `node -v` | Runtimes for Vite & React |
| **npm** | 9+ | `npm -v` | Package manager |
| **MySQL Server** | 8.0.x | `mysql --version` | InnoDB engine with timezone support |

---

## 2. Database Environment Setup

BorrowHub uses MySQL 8.0 as its central time arbiter (`Asia/Kolkata` `+05:30`).

### Configuration Parameters
- **Port:** `3306`
- **Database Name:** `borrowhub_db`
- **Username:** `borrowhub_user`
- **Password:** Configured via `DB_PASSWORD` in your local `.env`
- **Timezone:** `+05:30`

### Step 1: Configure Environment (.env)
Copy the example template to create your local `.env`:
```cmd
copy .env.example .env
```
Open `.env` and replace all placeholder values with your unique local passwords and a secure JWT secret:
- `DB_PASSWORD`: Password for your local MySQL database user.
- `JWT_SECRET`: Random string of at least 32 bytes (256-bit).
- `BORROWHUB_SEED_*_PASSWORD`: Passwords for local demo accounts (`admin`, `faculty1`, `staff1`, `student1`).

### Step 2: Automated Database Startup
Run from project root:
```cmd
scripts\start-database.bat
```

### Manual Database Creation (if configuring custom MySQL server):
```sql
CREATE DATABASE IF NOT EXISTS borrowhub_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'borrowhub_user'@'localhost' IDENTIFIED BY '<your-db-password>';
GRANT ALL PRIVILEGES ON borrowhub_db.* TO 'borrowhub_user'@'localhost';
FLUSH PRIVILEGES;
```

---

## 3. Quickstart: 1-Click Windows Automation

BorrowHub includes automated `.bat` scripts in `scripts/`:

```cmd
# 1. Start Database, Backend, and Frontend (opens browser automatically)
scripts\start-borrowhub.bat

# 2. Check System Health
scripts\health-check.bat

# 3. Stop All BorrowHub Services
scripts\stop-borrowhub.bat

# 4. Clean Restart
scripts\restart-borrowhub.bat
```

---

## 4. Manual Startup (Development Mode)

If you prefer running services in dedicated developer terminal windows:

### Terminal 1: Backend Service
```bash
cd borrowhub-backend
mvn spring-boot:run
```
- Listens on `http://localhost:8080`
- Actuator Health: `http://localhost:8080/actuator/health`

### Terminal 2: Frontend Web Client
```bash
cd borrowhub-frontend
npm install
npm run dev
```
- Listens on `http://localhost:5173`
- Proxies `/api` requests to `http://localhost:8080`

---

## 5. Seed Accounts & Local Authentication

When starting with an empty database, `DataInitializer` seeds four initial demonstration accounts using the credentials specified in your local `.env`:

| Role | Username | Password Environment Variable | Purpose |
|---|---|---|---|
| **Admin** | `admin` | `BORROWHUB_SEED_ADMIN_PASSWORD` | Master control, approvals, circulation desk |
| **Faculty** | `faculty1` | `BORROWHUB_SEED_FACULTY_PASSWORD` | Priority borrowing, lab equipment requests |
| **Staff** | `staff1` | `BORROWHUB_SEED_STAFF_PASSWORD` | Circulation desk, checkouts, returns |
| **Student** | `student1` | `BORROWHUB_SEED_STUDENT_PASSWORD` | Student borrowing, catalog holds, waitlist |

> **Security Note:** Seed credentials are local-only and must never be committed to source control or reused across environments. Always set unique, strong passwords in `.env`.

---

## 6. Troubleshooting Common Issues

### Issue 1: Port 8080 or 5173 Already in Use
Run:
```cmd
scripts\stop-borrowhub.bat
```
This gracefully terminates orphaned BorrowHub processes and releases ports.

### Issue 2: MySQL Timezone Mismatch
BorrowHub requires the MySQL server session to operate in `+05:30` (`Asia/Kolkata`). Ensure MySQL is started with:
```cmd
--default-time-zone="+05:30"
```
Or execute:
```sql
SET GLOBAL time_zone = '+05:30';
```
