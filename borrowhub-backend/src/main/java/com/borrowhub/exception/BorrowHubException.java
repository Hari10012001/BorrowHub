package com.borrowhub.exception;

public class BorrowHubException extends RuntimeException {

    private final String errorCode;

    public BorrowHubException(String message) {
        super(message);
        this.errorCode = "INTERNAL_SERVER_ERROR";
    }

    public BorrowHubException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BorrowHubException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
