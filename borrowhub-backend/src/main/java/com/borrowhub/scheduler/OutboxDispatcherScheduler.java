package com.borrowhub.scheduler;

import com.borrowhub.repository.OutboxEventRepository;
import com.borrowhub.service.outbox.OutboxSingleEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Background outbox dispatcher scheduler.
 * Enforces C47: Just-In-Time (JIT) single event claim and execution.
 */
@Component
public class OutboxDispatcherScheduler {

    private static final Logger logger = LoggerFactory.getLogger(OutboxDispatcherScheduler.class);

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxSingleEventExecutor singleEventExecutor;

    public OutboxDispatcherScheduler(OutboxEventRepository outboxEventRepository,
                                     OutboxSingleEventExecutor singleEventExecutor) {
        this.outboxEventRepository = outboxEventRepository;
        this.singleEventExecutor = singleEventExecutor;
    }

    @Scheduled(fixedDelayString = "${borrowhub.outbox.dispatcher-delay-ms:2000}")
    public void dispatchPendingEvents() {
        int processedCount = 0;
        int maxPerCycle = 20;

        while (processedCount < maxPerCycle) {
            Optional<Long> nextId = outboxEventRepository.findNextClaimableEventId();
            if (nextId.isEmpty()) {
                break; // No more events ready for claim
            }

            boolean processed = singleEventExecutor.processSingleEvent(nextId.get());
            if (processed) {
                processedCount++;
            } else {
                break;
            }
        }

        if (processedCount > 0) {
            logger.debug("Outbox cycle processed {} events", processedCount);
        }
    }
}
