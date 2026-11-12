package com.borrowhub.payload.response;

import com.borrowhub.model.enums.InventoryMode;

import java.time.Instant;

public class ResourceResponse {

    private Long id;
    private String name;
    private String resourceCode;
    private String description;
    private Long categoryId;
    private String categoryName;
    private InventoryMode inventoryMode;
    private Integer totalQuantity;
    private Integer availableQuantity;
    private Integer maxBorrowDays;
    private Boolean isReservable;
    private Boolean isActive;
    private String imageUrl;
    private Instant createdAt;
    private Instant updatedAt;

    public ResourceResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getResourceCode() { return resourceCode; }
    public void setResourceCode(String resourceCode) { this.resourceCode = resourceCode; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public InventoryMode getInventoryMode() { return inventoryMode; }
    public void setInventoryMode(InventoryMode inventoryMode) { this.inventoryMode = inventoryMode; }

    public Integer getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }

    public Integer getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(Integer availableQuantity) { this.availableQuantity = availableQuantity; }

    public Integer getMaxBorrowDays() { return maxBorrowDays; }
    public void setMaxBorrowDays(Integer maxBorrowDays) { this.maxBorrowDays = maxBorrowDays; }

    public Boolean getIsReservable() { return isReservable; }
    public void setIsReservable(Boolean reservable) { isReservable = reservable; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean active) { isActive = active; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
