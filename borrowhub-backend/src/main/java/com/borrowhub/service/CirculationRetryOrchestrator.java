package com.borrowhub.service;

import com.borrowhub.model.enums.FailureType;
import com.borrowhub.payload.request.CheckoutRequestDto;
import com.borrowhub.payload.request.ReturnRequestDto;
import com.borrowhub.payload.response.CheckoutResponseDto;
import com.borrowhub.payload.response.ReturnResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * Non-transactional Retry Orchestrator.
 * Enforces C50:
 * - Isolated from transactional boundary
 * - `@Retryable` wraps calls to `CirculationTransactionalService`
 * - Delegates each attempt to a fresh transaction via Spring AOP proxy
 * - Coordinates Level 0 Idempotency (C23, C26)
 */
@Service
public class CirculationRetryOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(CirculationRetryOrchestrator.class);

    private final CirculationTransactionalService transactionalService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public CirculationRetryOrchestrator(CirculationTransactionalService transactionalService,
                                        IdempotencyService idempotencyService,
                                        ObjectMapper objectMapper) {
        this.transactionalService = transactionalService;
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    @Retryable(
            retryFor = {PessimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2.0)
    )
    public CheckoutResponseDto checkout(CheckoutRequestDto dto, Long staffUserId, String idempotencyKey) {
        IdempotencyService.IdempotencyContext ctx = null;
        try {
            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                String rawBody = objectMapper.writeValueAsString(dto);
                ctx = idempotencyService.acquireOrReplay(idempotencyKey, staffUserId, "/api/circulation/checkout", rawBody);

                if (ctx != null && ctx.isReplayed()) {
                    logger.info("Replaying cached checkout response for key: {}", idempotencyKey);
                    return objectMapper.readValue(ctx.getCachedBody(), CheckoutResponseDto.class);
                }
            }

            CheckoutResponseDto response = transactionalService.executeCheckout(dto, staffUserId);

            if (ctx != null) {
                idempotencyService.complete(ctx, 200, objectMapper.writeValueAsString(response));
            }
            return response;

        } catch (PessimisticLockingFailureException e) {
            logger.warn("Transient lock contention during checkout, triggering retry: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            if (ctx != null) {
                idempotencyService.recordFailure(ctx, FailureType.TERMINAL);
            }
            if (e instanceof RuntimeException re) throw re;
            throw new RuntimeException(e);
        }
    }

    @Retryable(
            retryFor = {PessimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2.0)
    )
    public ReturnResponseDto processReturn(ReturnRequestDto dto, Long staffUserId, String idempotencyKey) {
        IdempotencyService.IdempotencyContext ctx = null;
        try {
            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                String rawBody = objectMapper.writeValueAsString(dto);
                ctx = idempotencyService.acquireOrReplay(idempotencyKey, staffUserId, "/api/circulation/return", rawBody);

                if (ctx != null && ctx.isReplayed()) {
                    logger.info("Replaying cached return response for key: {}", idempotencyKey);
                    return objectMapper.readValue(ctx.getCachedBody(), ReturnResponseDto.class);
                }
            }

            ReturnResponseDto response = transactionalService.executeReturn(dto, staffUserId);

            if (ctx != null) {
                idempotencyService.complete(ctx, 200, objectMapper.writeValueAsString(response));
            }
            return response;

        } catch (PessimisticLockingFailureException e) {
            logger.warn("Transient lock contention during return, triggering retry: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            if (ctx != null) {
                idempotencyService.recordFailure(ctx, FailureType.TERMINAL);
            }
            if (e instanceof RuntimeException re) throw re;
            throw new RuntimeException(e);
        }
    }

    @Recover
    public CheckoutResponseDto recoverCheckout(PessimisticLockingFailureException e, CheckoutRequestDto dto, Long staffUserId, String idempotencyKey) {
        logger.error("Checkout exhausted retry attempts due to lock contention: {}", e.getMessage());
        throw e;
    }

    @Recover
    public ReturnResponseDto recoverReturn(PessimisticLockingFailureException e, ReturnRequestDto dto, Long staffUserId, String idempotencyKey) {
        logger.error("Return exhausted retry attempts due to lock contention: {}", e.getMessage());
        throw e;
    }
}
