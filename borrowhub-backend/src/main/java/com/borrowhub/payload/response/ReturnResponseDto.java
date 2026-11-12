package com.borrowhub.payload.response;

import com.borrowhub.model.enums.BorrowingStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ReturnResponseDto {

    private Long borrowingRecordId;
    private Long resourceId;
    private String resourceName;
    private Integer quantityIssued;
    private Integer quantityReturned;
    private Integer quantityLost;
    private Integer quantityOutstanding;
    private BorrowingStatus status;
    private LocalDate actualReturnDate;
    private LocalDateTime closedAt;
    private boolean fullyReturned;
    private String message;

    public ReturnResponseDto() {}

    public Long getBorrowingRecordId() {
        return borrowingRecordId;
    }

    public void setBorrowingRecordId(Long borrowingRecordId) {
        this.borrowingRecordId = borrowingRecordId;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public Integer getQuantityIssued() {
        return quantityIssued;
    }

    public void setQuantityIssued(Integer quantityIssued) {
        this.quantityIssued = quantityIssued;
    }

    public Integer getQuantityReturned() {
        return quantityReturned;
    }

    public void setQuantityReturned(Integer quantityReturned) {
        this.quantityReturned = quantityReturned;
    }

    public Integer getQuantityLost() {
        return quantityLost;
    }

    public void setQuantityLost(Integer quantityLost) {
        this.quantityLost = quantityLost;
    }

    public Integer getQuantityOutstanding() {
        return quantityOutstanding;
    }

    public void setQuantityOutstanding(Integer quantityOutstanding) {
        this.quantityOutstanding = quantityOutstanding;
    }

    public BorrowingStatus getStatus() {
        return status;
    }

    public void setStatus(BorrowingStatus status) {
        this.status = status;
    }

    public LocalDate getActualReturnDate() {
        return actualReturnDate;
    }

    public void setActualReturnDate(LocalDate actualReturnDate) {
        this.actualReturnDate = actualReturnDate;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public boolean isFullyReturned() {
        return fullyReturned;
    }

    public void setFullyReturned(boolean fullyReturned) {
        this.fullyReturned = fullyReturned;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
