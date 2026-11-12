package com.borrowhub.service;

import com.borrowhub.exception.BadRequestException;
import com.borrowhub.exception.ConflictException;
import com.borrowhub.model.entity.IdempotencyRecord;
import com.borrowhub.model.enums.FailureType;
import com.borrowhub.model.enums.IdempotencyStatus;
import com.borrowhub.repository.IdempotencyRecordRepository;
import com.borrowhub.util.CanonicalJsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class IdempotencyService {

    private static final Logger logger = LoggerFactory.getLogger(IdempotencyService.class);
    private static final int DEFAULT_LEASE_SECONDS = 60;

    private final IdempotencyRecordRepository idempotencyRecordRepository;

    public IdempotencyService(IdempotencyRecordRepository idempotencyRecordRepository) {
        this.idempotencyRecordRepository = idempotencyRecordRepository;
    }

    public static class IdempotencyContext {
        private final String key;
        private final String ownerToken;
        private final long fenceGeneration;
        private final boolean replayed;
        private final Integer cachedStatus;
        private final String cachedBody;

        public IdempotencyContext(String key, String ownerToken, long fenceGeneration, boolean replayed, Integer cachedStatus, String cachedBody) {
            this.key = key;
            this.ownerToken = ownerToken;
            this.fenceGeneration = fenceGeneration;
            this.replayed = replayed;
            this.cachedStatus = cachedStatus;
            this.cachedBody = cachedBody;
        }

        public String getKey() { return key; }
        public String getOwnerToken() { return ownerToken; }
        public long getFenceGeneration() { return fenceGeneration; }
        public boolean isReplayed() { return replayed; }
        public Integer getCachedStatus() { return cachedStatus; }
        public String getCachedBody() { return cachedBody; }
    }

    /**
     * Level 0 idempotency acquisition in a dedicated transaction.
     * Enforces C23 / C26 constraints:
     * - Authenticated user ownership check
     * - Endpoint verification
     * - Canonical request hash verification
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IdempotencyContext acquireOrReplay(String key, Long userId, String endpoint, String rawBody) {
        if (key == null || key.isBlank()) {
            return null; // Idempotency key not provided, proceed normally
        }

        String requestHash = CanonicalJsonUtil.computeRequestHash(endpoint, userId, rawBody);
        Optional<IdempotencyRecord> optionalRecord = idempotencyRecordRepository.findByIdForUpdate(key);

        if (optionalRecord.isPresent()) {
            IdempotencyRecord record = optionalRecord.get();

            // C23: Validate ownership
            if (!record.getUserId().equals(userId)) {
                logger.warn("Idempotency key reuse attempt by different user. Expected: {}, Provided: {}", record.getUserId(), userId);
                throw new BadRequestException("IDEMPOTENCY_OWNERSHIP_MISMATCH", "Idempotency key belongs to another user");
            }

            // C23: Validate endpoint
            if (!record.getEndpoint().equals(endpoint)) {
                logger.warn("Idempotency key reuse attempt on different endpoint. Expected: {}, Provided: {}", record.getEndpoint(), endpoint);
                throw new BadRequestException("IDEMPOTENCY_ENDPOINT_MISMATCH", "Idempotency key reused on different endpoint");
            }

            // C23: Validate payload hash
            if (!record.getRequestHash().equals(requestHash)) {
                logger.warn("Idempotency key payload mismatch for key: {}", key);
                throw new ConflictException("IDEMPOTENCY_PAYLOAD_MISMATCH", "Idempotency key reused with different request payload");
            }

            // If already COMPLETED, replay cached response
            if (record.getStatus() == IdempotencyStatus.COMPLETED) {
                logger.info("Replaying completed idempotency record for key: {}", key);
                return new IdempotencyContext(key, null, record.getFenceGeneration(), true, record.getResponseStatus(), record.getResponseBody());
            }

            // If IN_PROGRESS, check lease
            if (record.getStatus() == IdempotencyStatus.IN_PROGRESS) {
                if (record.getLeaseExpiresAt().isAfter(Instant.now())) {
                    throw new ConflictException("CONCURRENT_OPERATION", "Operation with idempotency key is currently in progress");
                }
                // Lease expired: acquire it
                String ownerToken = UUID.randomUUID().toString();
                record.setOwnerToken(ownerToken);
                record.setFenceGeneration(record.getFenceGeneration() + 1);
                record.setLeaseExpiresAt(Instant.now().plus(DEFAULT_LEASE_SECONDS, ChronoUnit.SECONDS));
                idempotencyRecordRepository.save(record);
                return new IdempotencyContext(key, ownerToken, record.getFenceGeneration(), false, null, null);
            }

            // If failed but retryable
            if (record.getStatus() == IdempotencyStatus.FAILED && record.getFailureType() == FailureType.RETRYABLE) {
                String ownerToken = UUID.randomUUID().toString();
                record.setStatus(IdempotencyStatus.IN_PROGRESS);
                record.setOwnerToken(ownerToken);
                record.setFenceGeneration(record.getFenceGeneration() + 1);
                record.setLeaseExpiresAt(Instant.now().plus(DEFAULT_LEASE_SECONDS, ChronoUnit.SECONDS));
                idempotencyRecordRepository.save(record);
                return new IdempotencyContext(key, ownerToken, record.getFenceGeneration(), false, null, null);
            }

            // Permanent failure
            throw new ConflictException("IDEMPOTENCY_FAILED_PERMANENTLY", "Previous operation with this idempotency key failed permanently");
        }

        // New record
        String ownerToken = UUID.randomUUID().toString();
        IdempotencyRecord newRecord = new IdempotencyRecord();
        newRecord.setIdempotencyKey(key);
        newRecord.setUserId(userId);
        newRecord.setEndpoint(endpoint);
        newRecord.setRequestHash(requestHash);
        newRecord.setStatus(IdempotencyStatus.IN_PROGRESS);
        newRecord.setOwnerToken(ownerToken);
        newRecord.setFenceGeneration(0L);
        newRecord.setLeaseExpiresAt(Instant.now().plus(DEFAULT_LEASE_SECONDS, ChronoUnit.SECONDS));
        idempotencyRecordRepository.save(newRecord);

        return new IdempotencyContext(key, ownerToken, 0L, false, null, null);
    }

    /**
     * Mark completion with C48 conditional fencing guard.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(IdempotencyContext context, int responseStatus, String responseBody) {
        if (context == null || context.getKey() == null || context.isReplayed()) {
            return;
        }

        int updated = idempotencyRecordRepository.markCompletedWithFencing(
                context.getKey(),
                context.getOwnerToken(),
                context.getFenceGeneration(),
                responseStatus,
                responseBody
        );

        if (updated == 0) {
            logger.warn("Idempotency completion guard rejected update for key: {}. Fence generation or lease ownership expired.", context.getKey());
        }
    }

    /**
     * Record failure for an in-flight idempotency record.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(IdempotencyContext context, FailureType failureType) {
        if (context == null || context.getKey() == null || context.isReplayed()) {
            return;
        }

        Optional<IdempotencyRecord> opt = idempotencyRecordRepository.findById(context.getKey());
        if (opt.isPresent()) {
            IdempotencyRecord record = opt.get();
            if (context.getOwnerToken().equals(record.getOwnerToken())) {
                record.setStatus(IdempotencyStatus.FAILED);
                record.setFailureType(failureType);
                record.setOwnerToken(null);
                idempotencyRecordRepository.save(record);
            }
        }
    }
}
