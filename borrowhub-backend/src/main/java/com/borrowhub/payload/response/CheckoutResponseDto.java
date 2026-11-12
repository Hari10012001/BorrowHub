package com.borrowhub.payload.response;

import com.borrowhub.model.enums.BorrowingStatus;

import java.time.LocalDate;
import java.util.List;

public class CheckoutResponseDto {

    private Long borrowingRecordId;
    private Long resourceId;
    private String resourceName;
    private Long userId;
    private String userName;
    private Integer quantityIssued;
    private Integer quantityOutstanding;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private BorrowingStatus status;
    private List<String> issuedAssetTags;
    private Long reservationId;
    private Integer reservationRemainderReleased;

    public CheckoutResponseDto() {}

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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Integer getQuantityIssued() {
        return quantityIssued;
    }

    public void setQuantityIssued(Integer quantityIssued) {
        this.quantityIssued = quantityIssued;
    }

    public Integer getQuantityOutstanding() {
        return quantityOutstanding;
    }

    public void setQuantityOutstanding(Integer quantityOutstanding) {
        this.quantityOutstanding = quantityOutstanding;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public BorrowingStatus getStatus() {
        return status;
    }

    public void setStatus(BorrowingStatus status) {
        this.status = status;
    }

    public List<String> getIssuedAssetTags() {
        return issuedAssetTags;
    }

    public void setIssuedAssetTags(List<String> issuedAssetTags) {
        this.issuedAssetTags = issuedAssetTags;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public Integer getReservationRemainderReleased() {
        return reservationRemainderReleased;
    }

    public void setReservationRemainderReleased(Integer reservationRemainderReleased) {
        this.reservationRemainderReleased = reservationRemainderReleased;
    }
}
