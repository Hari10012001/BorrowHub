package com.borrowhub.service.outbox;

import com.borrowhub.model.entity.OutboxEvent;

public interface OutboxEventHandler {
    boolean supports(String eventType);
    void handle(OutboxEvent event) throws Exception;
}
