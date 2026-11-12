package com.borrowhub.payload.response;

import com.borrowhub.model.enums.ReservationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ReservationResponseDto {

    private Long id;
    private Long resourceId;
    private String resourceName;
    private Long userId;
    private String userName;
    private Integer quantityReserved;
    private LocalDate startDate;
    private LocalDate endDate;
    private ReservationStatus status;
    private LocalDateTime holdExpiresAt;
    private List<Long> reservedUnitIds;
    private List<String> reservedAssetTags;
    private LocalDateTime createdAt;

    public ReservationResponseDto() {}

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

    public Integer getQuantityReserved() {
        return quantityReserved;
    }

    public void setQuantityReserved(Integer quantityReserved) {
        this.quantityReserved = quantityReserved;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public LocalDateTime getHoldExpiresAt() {
        return holdExpiresAt;
    }

    public void setHoldExpiresAt(LocalDateTime holdExpiresAt) {
        this.holdExpiresAt = holdExpiresAt;
    }

    public List<Long> getReservedUnitIds() {
        return reservedUnitIds;
    }

    public void setReservedUnitIds(List<Long> reservedUnitIds) {
        this.reservedUnitIds = reservedUnitIds;
    }

    public List<String> getReservedAssetTags() {
        return reservedAssetTags;
    }

    public void setReservedAssetTags(List<String> reservedAssetTags) {
        this.reservedAssetTags = reservedAssetTags;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
