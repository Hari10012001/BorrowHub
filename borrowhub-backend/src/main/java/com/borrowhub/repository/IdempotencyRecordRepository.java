package com.borrowhub.repository;

import com.borrowhub.model.entity.IdempotencyRecord;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, String> {

    // Level 0 Lock (DAG Hierarchy)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ir FROM IdempotencyRecord ir WHERE ir.idempotencyKey = :key")
    Optional<IdempotencyRecord> findByIdForUpdate(@Param("key") String key);

    @Modifying
    @Query(value = "UPDATE idempotency_records " +
                   "SET status = 'IN_PROGRESS', " +
                   "    owner_token = :ownerToken, " +
                   "    fence_generation = fence_generation + 1, " +
                   "    lease_expires_at = DATE_ADD(NOW(6), INTERVAL :leaseSeconds SECOND), " +
                   "    updated_at = NOW(6) " +
                   "WHERE idempotency_key = :key " +
                   "  AND ( " +
                   "       (status = 'IN_PROGRESS' AND lease_expires_at < NOW(6)) " +
                   "    OR (status = 'FAILED' AND failure_type = 'RETRYABLE') " +
                   "  )", nativeQuery = true)
    int acquireExpiredOrRetryableLease(
            @Param("key") String key,
            @Param("ownerToken") String ownerToken,
            @Param("leaseSeconds") int leaseSeconds
    );

    @Modifying
    @Query(value = "UPDATE idempotency_records " +
                   "SET status = 'COMPLETED', " +
                   "    response_status = :status, " +
                   "    response_body = :body, " +
                   "    owner_token = NULL, " +
                   "    updated_at = NOW(6) " +
                   "WHERE idempotency_key = :key " +
                   "  AND owner_token = :ownerToken " +
                   "  AND fence_generation = :fenceGeneration " +
                   "  AND status = 'IN_PROGRESS'", nativeQuery = true)
    int markCompletedWithFencing(
            @Param("key") String key,
            @Param("ownerToken") String ownerToken,
            @Param("fenceGeneration") long fenceGeneration,
            @Param("status") int status,
            @Param("body") String body
    );

    @Modifying
    @Query(value = "UPDATE idempotency_records " +
                   "SET status = 'FAILED', " +
                   "    failure_type = :failureType, " +
                   "    response_status = :status, " +
                   "    response_body = :body, " +
                   "    owner_token = NULL, " +
                   "    updated_at = NOW(6) " +
                   "WHERE idempotency_key = :key " +
                   "  AND owner_token = :ownerToken " +
                   "  AND fence_generation = :fenceGeneration " +
                   "  AND status = 'IN_PROGRESS'", nativeQuery = true)
    int markFailedWithFencing(
            @Param("key") String key,
            @Param("ownerToken") String ownerToken,
            @Param("fenceGeneration") long fenceGeneration,
            @Param("failureType") String failureType,
            @Param("status") int status,
            @Param("body") String body
    );
}
