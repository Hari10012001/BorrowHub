package com.borrowhub.service.outbox;

import com.borrowhub.model.entity.OutboxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BorrowingOverdueHandler implements OutboxEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(BorrowingOverdueHandler.class);

    @Override
    public boolean supports(String eventType) {
        return "BORROWING_OVERDUE".equals(eventType);
    }

    @Override
    public void handle(OutboxEvent event) throws Exception {
        logger.warn("Processing BORROWING_OVERDUE notification for borrowing record id: {}", event.getAggregateId());
    }
}
