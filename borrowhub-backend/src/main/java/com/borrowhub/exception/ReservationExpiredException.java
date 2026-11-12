package com.borrowhub.exception;

public class ReservationExpiredException extends BorrowHubException {

    public ReservationExpiredException(String message) {
        super("RESERVATION_EXPIRED", message);
    }
}
