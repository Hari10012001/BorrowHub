package com.borrowhub.exception;

public class InvalidStatusTransitionException extends BorrowHubException {

    public InvalidStatusTransitionException(String entityName, String fromStatus, String toStatus) {
        super("INVALID_STATUS_TRANSITION",
                String.format("Invalid status transition for %s from %s to %s", entityName, fromStatus, toStatus));
    }

    public InvalidStatusTransitionException(String message) {
        super("INVALID_STATUS_TRANSITION", message);
    }
}
