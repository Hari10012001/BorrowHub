package com.borrowhub.payload.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class WaitlistJoinRequest {

    @NotNull(message = "Resource ID is required")
    private Long resourceId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotNull(message = "Needed from date is required")
    private LocalDate neededFrom;

    @NotNull(message = "Needed until date is required")
    @Future(message = "Needed until date must be in the future")
    private LocalDate neededUntil;

    public WaitlistJoinRequest() {}

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public LocalDate getNeededFrom() {
        return neededFrom;
    }

    public void setNeededFrom(LocalDate neededFrom) {
        this.neededFrom = neededFrom;
    }

    public LocalDate getNeededUntil() {
        return neededUntil;
    }

    public void setNeededUntil(LocalDate neededUntil) {
        this.neededUntil = neededUntil;
    }
}
