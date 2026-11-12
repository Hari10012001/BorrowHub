package com.borrowhub.model.enums;

public enum OutboxStatus {
    PENDING,
    IN_FLIGHT,
    PROCESSED,
    DEAD_LETTER
}
