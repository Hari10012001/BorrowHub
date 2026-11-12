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
- **Password:** `BorrowHub@2026`
- **Timezone:** `+05:30`

### Automated Database Startup
Run from project root:
```cmd
scripts\start-database.bat
```

### Manual Database Creation (if configuring custom MySQL server):
```sql
CREATE DATABASE IF NOT EXISTS borrowhub_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'borrowhub_user'@'localhost' IDENTIFIED BY 'BorrowHub@2026';
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

## 5. Seed Accounts & Default Logins

The application initializes with three pre-configured accounts:

| Role | Username | Password | Purpose |
|---|---|---|---|
| **Admin** | `admin` | `Admin@123` | Master control, approvals, circulation desk |
| **Faculty** | `faculty1` | `Faculty@123` | Priority borrowing, lab equipment requests |
| **Student** | `student1` | `Student@123` | Student borrowing, catalog holds, waitlist |

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
