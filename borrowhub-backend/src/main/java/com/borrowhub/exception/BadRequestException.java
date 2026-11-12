package com.borrowhub.exception;

public class BadRequestException extends BorrowHubException {

    public BadRequestException(String message) {
        super("BAD_REQUEST", message);
    }

    public BadRequestException(String errorCode, String message) {
        super(errorCode, message);
    }
}
