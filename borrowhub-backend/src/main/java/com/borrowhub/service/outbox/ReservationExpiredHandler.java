package com.borrowhub.service.outbox;

import com.borrowhub.model.entity.OutboxEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ReservationExpiredHandler implements OutboxEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(ReservationExpiredHandler.class);

    @Override
    public boolean supports(String eventType) {
        return "RESERVATION_EXPIRED".equals(eventType);
    }

    @Override
    public void handle(OutboxEvent event) throws Exception {
        logger.info("Processing RESERVATION_EXPIRED event id: {}, payload: {}", event.getId(), event.getPayload());
        // System notification / audit record logged
    }
}
