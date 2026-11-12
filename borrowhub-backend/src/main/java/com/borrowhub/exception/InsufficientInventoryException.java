package com.borrowhub.exception;

public class InsufficientInventoryException extends BorrowHubException {

    public InsufficientInventoryException(String message) {
        super("INSUFFICIENT_INVENTORY", message);
    }
}
