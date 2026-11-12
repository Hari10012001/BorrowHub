package com.borrowhub.exception;

public class ConflictException extends BorrowHubException {

    public ConflictException(String message) {
        super("CONFLICT", message);
    }

    public ConflictException(String errorCode, String message) {
        super(errorCode, message);
    }
}
