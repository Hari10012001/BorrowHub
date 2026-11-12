package com.borrowhub.payload.response;

import com.borrowhub.model.enums.WaitlistStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class WaitlistEntryResponseDto {

    private Long id;
    private Long resourceId;
    private String resourceName;
    private Long userId;
    private String userName;
    private Integer quantity;
    private Integer priorityTier;
    private LocalDate neededFrom;
    private LocalDate neededUntil;
    private WaitlistStatus status;
    private Integer positionInQueue;
    private LocalDateTime notifiedAt;
    private LocalDateTime claimDeadline;
    private Long allocatedReservationId;
    private LocalDateTime createdAt;

    public WaitlistEntryResponseDto() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getPriorityTier() {
        return priorityTier;
    }

    public void setPriorityTier(Integer priorityTier) {
        this.priorityTier = priorityTier;
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

    public WaitlistStatus getStatus() {
        return status;
    }

    public void setStatus(WaitlistStatus status) {
        this.status = status;
    }

    public Integer getPositionInQueue() {
        return positionInQueue;
    }

    public void setPositionInQueue(Integer positionInQueue) {
        this.positionInQueue = positionInQueue;
    }

    public LocalDateTime getNotifiedAt() {
        return notifiedAt;
    }

    public void setNotifiedAt(LocalDateTime notifiedAt) {
        this.notifiedAt = notifiedAt;
    }

    public LocalDateTime getClaimDeadline() {
        return claimDeadline;
    }

    public void setClaimDeadline(LocalDateTime claimDeadline) {
        this.claimDeadline = claimDeadline;
    }

    public Long getAllocatedReservationId() {
        return allocatedReservationId;
    }

    public void setAllocatedReservationId(Long allocatedReservationId) {
        this.allocatedReservationId = allocatedReservationId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
