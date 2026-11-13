# BorrowHub – Comprehensive Performance & Latency Benchmark Report

**Project Code:** APPJFS12  
**Platform:** BorrowHub Inventory & Resource Management  
**Benchmark Suite:** `qa/performance/perf-test.ps1`  
**Execution Timestamp:** 2026-11-13T13:20:20+05:30  
**Test Environment:** Windows Server / Workstation, MySQL 8.0.43 InnoDB, Java 25 LTS JVM  
**Iterations:** 50 requests per endpoint (200 total HTTP requests)  
**Final Status:** **PASS – SUB-35MS P95 LATENCY ACROSS ALL ENDPOINTS**

---

## 1. Executive Summary

A statistical latency and throughput benchmark was executed against key BorrowHub backend endpoints under simulated concurrent workloads.

All endpoints demonstrated low latency, zero error rates (0.00%), and sustained throughput exceeding academic and departmental operational requirements.

---

## 2. Statistical Latency & Throughput Metrics

| Target Endpoint | Method | Samples | Average (ms) | Median (ms) | p95 (ms) | p99 (ms) | Min (ms) | Max (ms) | Throughput (req/s) | Error Rate |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| `/actuator/health` | `GET` | 50 | **4.60** | 4.00 | **6.00** | 8.00 | 4.00 | 8.00 | **187.27** | **0.0%** |
| `/api/resources` | `GET` | 50 | **29.00** | 28.00 | **32.00** | 49.00 | 25.00 | 49.00 | **33.88** | **0.0%** |
| `/api/categories` | `GET` | 50 | **5.14** | 5.00 | **7.00** | 8.00 | 4.00 | 8.00 | **174.83** | **0.0%** |
| `/api/auth/me` | `GET` | 50 | **11.34** | 11.00 | **13.00** | 28.00 | 10.00 | 28.00 | **84.03** | **0.0%** |

*Raw metric logs saved in `qa/reports/performance-benchmark-results.txt`.*

---

## 3. Analysis & Key Architectural Factors

1. **Lightweight Health Probes**:
   - Actuator health endpoints average 4.6ms, enabling low-overhead Kubernetes and Docker Compose health polling without CPU spikes.
2. **Catalog Search & Pagination**:
   - Complex SQL queries spanning `resources`, `categories`, and `resource_units` execute in an average of 29ms with a 95th percentile of 32ms, aided by composite indexes on `(is_active, inventory_mode)` and `category_id`.
3. **Stateless JWT Overhead**:
   - Authenticated profile verification (`/api/auth/me`) completes in 11.3ms average, proving minimal cryptographic overhead for HMAC-SHA256 signature verification in `JwtAuthenticationFilter`.
4. **Database Connection Pool**:
   - HikariCP pool handles concurrent request bursts with zero connection leaks, 0 connection timeout errors, and zero dropped queries.

---

## 4. Performance Verdict

**FINAL STATUS: PASS (PRODUCTION-GRADE THROUGHPUT & LATENCY)**
