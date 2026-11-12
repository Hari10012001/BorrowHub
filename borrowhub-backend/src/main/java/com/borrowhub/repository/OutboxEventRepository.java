package com.borrowhub.repository;

import com.borrowhub.model.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    // JIT Single-Event Claim Candidate Query (C47)
    @Query(value = "SELECT id FROM outbox_events " +
                   "WHERE (status = 'PENDING' AND (next_retry_at IS NULL OR next_retry_at <= NOW(6))) " +
                   "   OR (status = 'IN_FLIGHT' AND lease_expires_at < NOW(6)) " +
                   "ORDER BY created_at ASC " +
                   "LIMIT 1 " +
                   "FOR UPDATE SKIP LOCKED", nativeQuery = true)
    Optional<Long> findNextClaimableEventId();

    @Modifying
    @Query(value = "UPDATE outbox_events " +
                   "SET status = 'IN_FLIGHT', " +
                   "    claimed_by = :workerId, " +
                   "    claimed_at = NOW(6), " +
                   "    lease_expires_at = DATE_ADD(NOW(6), INTERVAL :leaseSeconds SECOND), " +
                   "    fence_generation = fence_generation + 1, " +
                   "    updated_at = NOW(6) " +
                   "WHERE id = :id", nativeQuery = true)
    int claimEvent(
            @Param("id") Long id,
            @Param("workerId") String workerId,
            @Param("leaseSeconds") int leaseSeconds
    );

    // Unified Conditional Completion Guard (C37, C44, C48, C49)
    @Modifying
    @Query(value = "UPDATE outbox_events " +
                   "SET status = 'PROCESSED', " +
                   "    processed_at = NOW(6), " +
                   "    claimed_by = NULL, " +
                   "    lease_expires_at = NULL, " +
                   "    updated_at = NOW(6) " +
                   "WHERE id = :eventId " +
                   "  AND claimed_by = :workerId " +
                   "  AND fence_generation = :expectedFenceGen " +
                   "  AND status = 'IN_FLIGHT' " +
                   "  AND lease_expires_at > NOW(6)", nativeQuery = true)
    int markCompletedWithFencing(
            @Param("eventId") Long eventId,
            @Param("workerId") String workerId,
            @Param("expectedFenceGen") long expectedFenceGen
    );

    @Modifying
    @Query(value = "UPDATE outbox_events " +
                   "SET status = 'PENDING', " +
                   "    retry_count = retry_count + 1, " +
                   "    next_retry_at = DATE_ADD(NOW(6), INTERVAL :backoffSeconds SECOND), " +
                   "    claimed_by = NULL, " +
                   "    lease_expires_at = NULL, " +
                   "    last_error = :errorMessage, " +
                   "    updated_at = NOW(6) " +
                   "WHERE id = :eventId " +
                   "  AND claimed_by = :workerId " +
                   "  AND fence_generation = :expectedFenceGen", nativeQuery = true)
    int scheduleRetry(
            @Param("eventId") Long eventId,
            @Param("workerId") String workerId,
            @Param("expectedFenceGen") long expectedFenceGen,
            @Param("backoffSeconds") int backoffSeconds,
            @Param("errorMessage") String errorMessage
    );

    @Modifying
    @Query(value = "UPDATE outbox_events " +
                   "SET status = 'DEAD_LETTER', " +
                   "    claimed_by = NULL, " +
                   "    lease_expires_at = NULL, " +
                   "    last_error = :errorMessage, " +
                   "    updated_at = NOW(6) " +
                   "WHERE id = :eventId " +
                   "  AND claimed_by = :workerId " +
                   "  AND fence_generation = :expectedFenceGen", nativeQuery = true)
    int markDeadLetter(
            @Param("eventId") Long eventId,
            @Param("workerId") String workerId,
            @Param("expectedFenceGen") long expectedFenceGen,
            @Param("errorMessage") String errorMessage
    );
}
