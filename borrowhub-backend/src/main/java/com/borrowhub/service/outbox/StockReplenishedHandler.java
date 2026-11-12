package com.borrowhub.service.outbox;

import com.borrowhub.model.entity.OutboxEvent;
import com.borrowhub.service.WaitlistService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class StockReplenishedHandler implements OutboxEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(StockReplenishedHandler.class);

    private final WaitlistService waitlistService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StockReplenishedHandler(WaitlistService waitlistService) {
        this.waitlistService = waitlistService;
    }

    @Override
    public boolean supports(String eventType) {
        return "STOCK_REPLENISHED".equals(eventType);
    }

    @Override
    public void handle(OutboxEvent event) throws Exception {
        JsonNode node = objectMapper.readTree(event.getPayload());
        long resourceId = node.has("resourceId") ? node.get("resourceId").asLong() : event.getAggregateId();

        logger.info("Handling STOCK_REPLENISHED for resourceId: {}", resourceId);
        boolean promoted = waitlistService.promoteNextInQueue(resourceId);
        if (promoted) {
            logger.info("Successfully promoted next waitlist candidate for resource: {}", resourceId);
        } else {
            logger.info("No waiting waitlist candidates found for resource: {}", resourceId);
        }
    }
}
