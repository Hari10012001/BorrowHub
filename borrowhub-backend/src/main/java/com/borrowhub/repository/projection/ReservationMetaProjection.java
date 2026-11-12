package com.borrowhub.repository.projection;

public interface ReservationMetaProjection {
    Long getId();
    String getReservationNumber();
    Long getResourceId();
    Integer getQuantity();
}
