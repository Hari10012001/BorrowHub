package com.borrowhub.service.outbox;

import com.borrowhub.model.entity.OutboxEvent;
import com.borrowhub.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Executes a single outbox event with conditional fencing and timeout handling.
 * Enforces C47, C48, and C49:
 * - Single-event claim lease
 * - Conditional completion guard checking claimed_by, fence_generation, and lease expiration
 * - Exponential backoff and dead lettering after 5 failures
 */
@Service
public class OutboxSingleEventExecutor {

    private static final Logger logger = LoggerFactory.getLogger(OutboxSingleEventExecutor.class);
    private static final int LEASE_SECONDS = 60;
    private static final int MAX_RETRIES = 5;

    private final String workerId = "worker-" + UUID.randomUUID().toString().substring(0, 8);
    private final OutboxEventRepository outboxEventRepository;
    private final List<OutboxEventHandler> eventHandlers;

    public OutboxSingleEventExecutor(OutboxEventRepository outboxEventRepository,
                                     List<OutboxEventHandler> eventHandlers) {
        this.outboxEventRepository = outboxEventRepository;
        this.eventHandlers = eventHandlers;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean processSingleEvent(Long eventId) {
        // C47 Step 1: Claim single event with lease
        int claimed = outboxEventRepository.claimEvent(eventId, workerId, LEASE_SECONDS);
        if (claimed == 0) {
            return false; // Another worker claimed this event or it is no longer claimable
        }

        OutboxEvent event = outboxEventRepository.findById(eventId).orElse(null);
        if (event == null) {
            return false;
        }

        long fenceGen = event.getFenceGeneration();

        try {
            // Find supporting handler
            OutboxEventHandler targetHandler = null;
            for (OutboxEventHandler handler : eventHandlers) {
                if (handler.supports(event.getEventType())) {
                    targetHandler = handler;
                    break;
                }
            }

            if (targetHandler != null) {
                targetHandler.handle(event);
            } else {
                logger.warn("No handler registered for outbox event type: {}", event.getEventType());
            }

            // C48: Unified Conditional Completion Guard
            int completed = outboxEventRepository.markCompletedWithFencing(eventId, workerId, fenceGen);
            if (completed == 0) {
                logger.warn("Outbox completion guard rejected event id {}. Fence gen or lease expired.", eventId);
            } else {
                logger.debug("Outbox event id {} completed successfully", eventId);
            }
            return true;

        } catch (Exception ex) {
            logger.error("Error processing outbox event id {}: {}", eventId, ex.getMessage(), ex);

            int retryCount = event.getRetryCount();
            if (retryCount >= MAX_RETRIES) {
                outboxEventRepository.markDeadLetter(eventId, workerId, fenceGen, ex.getMessage());
                logger.error("Outbox event id {} moved to DEAD_LETTER after {} failed attempts", eventId, retryCount);
            } else {
                int backoffSeconds = (int) Math.min(300, Math.pow(2, retryCount + 1));
                outboxEventRepository.scheduleRetry(eventId, workerId, fenceGen, backoffSeconds, ex.getMessage());
                logger.info("Outbox event id {} scheduled for retry in {} seconds", eventId, backoffSeconds);
            }
            return false;
        }
    }
}
