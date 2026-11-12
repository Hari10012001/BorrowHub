# BorrowHub — Backend Service

## Overview
The **borrowhub-backend** module is an enterprise Spring Boot 3.4.1 application written in Java 21 LTS. It provides REST APIs for user authentication, resource inventory, formal requests, reservations, waitlist queues, circulation desk operations, and transactional outbox event dispatching.

## Architecture Highlights
- **Framework:** Spring Boot 3.4.1
- **Security:** Spring Security 6 with stateless JWT authentication & RBAC.
- **Persistence:** Spring Data JPA with Hibernate 6.
- **Database Engine:** MySQL 8.0 InnoDB (Central Time Arbiter, `Asia/Kolkata` `+05:30`).
- **Migrations:** Flyway 10.
- **Idempotency Engine:** RFC 8785 JSON Canonicalization Scheme via `org.erdtman.jcs.JsonCanonicalizer` and SHA-256 fingerprinting.
- **Resilience:** Spring Retry & Spring Aspects for proxy-isolated transactional retries.
- **Concurrency Protection:** Strict DAG lock ordering (Levels 0–6) eliminating database deadlocks.

## Package Structure
```
com.borrowhub/
├── BorrowHubApplication.java
├── config/                  # SecurityConfig, CorsConfig, RetryConfig, DataInitializer
├── controller/              # REST Endpoints (Auth, Resource, Circulation, Request, Waitlist)
├── exception/               # GlobalExceptionHandler, Custom Exceptions
├── model/
│   ├── entity/              # 17 JPA Entities (zero JVM clock, fencing, stored generated columns)
│   └── enums/               # 15 Domain Enums
├── repository/              # 17 JPA Repositories (Pessimistic locking, DAG order)
├── scheduler/               # Outbox, Reservation Expiry, Overdue Schedulers
├── security/                # JwtAuthenticationFilter, JwtTokenProvider, UserPrincipal
├── service/                 # Domain Services (Idempotency, Circulation, Availability, etc.)
│   └── outbox/              # Event Handlers (StockReplenished, ReservationExpired, etc.)
└── util/                    # CanonicalJsonUtil (RFC 8785)
```

## Running & Testing
```bash
# Execute automated test suite
mvn test

# Package executable fat JAR
mvn clean package -DskipTests

# Run application
java -jar target/borrowhub-backend-1.0.0.jar
```
Port: `8080`  
Health Check: `http://localhost:8080/actuator/health`
