package com.borrowhub.service;

import com.borrowhub.model.entity.OutboxEvent;
import com.borrowhub.model.enums.OutboxStatus;
import com.borrowhub.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transactional Outbox publishing service.
 * Enforces Level 6 DAG lock ordering: events are inserted inside
 * the business transaction after business entity modifications.
 */
@Service
public class OutboxService {

    private static final Logger logger = LoggerFactory.getLogger(OutboxService.class);

    private final OutboxEventRepository outboxEventRepository;

    public OutboxService(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent publishEvent(String eventType, String aggregateType, Long aggregateId, String payload) {
        OutboxEvent event = new OutboxEvent();
        event.setEventType(eventType);
        event.setAggregateId(aggregateId);
        event.setPayload(payload);
        event.setStatus(OutboxStatus.PENDING);
        event.setRetryCount(0);
        event.setFenceGeneration(0L);

        OutboxEvent saved = outboxEventRepository.save(event);
        logger.debug("Published outbox event id: {}, type: {}, aggregateId: {}", saved.getId(), eventType, aggregateId);
        return saved;
    }
}
