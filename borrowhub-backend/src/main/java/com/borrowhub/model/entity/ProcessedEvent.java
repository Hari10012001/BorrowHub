package com.borrowhub.model.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "processed_events")
public class ProcessedEvent {

    @EmbeddedId
    private ProcessedEventId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("eventId")
    @JoinColumn(name = "event_id", nullable = false)
    private OutboxEvent outboxEvent;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt = Instant.now();

    public ProcessedEvent() {}

    public ProcessedEvent(OutboxEvent outboxEvent, String consumerName) {
        this.outboxEvent = outboxEvent;
        this.id = new ProcessedEventId(outboxEvent.getId(), consumerName);
        this.processedAt = Instant.now();
    }

    public ProcessedEventId getId() { return id; }
    public void setId(ProcessedEventId id) { this.id = id; }

    public OutboxEvent getOutboxEvent() { return outboxEvent; }
    public void setOutboxEvent(OutboxEvent outboxEvent) { this.outboxEvent = outboxEvent; }

    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
}
