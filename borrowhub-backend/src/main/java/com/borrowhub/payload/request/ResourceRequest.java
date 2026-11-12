package com.borrowhub.payload.request;

import com.borrowhub.model.enums.InventoryMode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ResourceRequest {

    @NotBlank(message = "Resource name is required")
    @Size(max = 150, message = "Name must not exceed 150 characters")
    private String name;

    @NotBlank(message = "Resource code is required")
    @Size(max = 50, message = "Code must not exceed 50 characters")
    private String resourceCode;

    private String description;

    @NotNull(message = "Category is required")
    private Long categoryId;

    @NotNull(message = "Inventory mode is required")
    private InventoryMode inventoryMode = InventoryMode.INDIVIDUAL;

    @Min(value = 0, message = "Total quantity cannot be negative")
    private Integer totalQuantity = 1;

    @Min(value = 1, message = "Max borrowing days must be at least 1")
    private Integer maxBorrowDays = 7;

    private Boolean isReservable = true;
    private Boolean isActive = true;
    private String imageUrl;

    public ResourceRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getResourceCode() { return resourceCode; }
    public void setResourceCode(String resourceCode) { this.resourceCode = resourceCode; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public InventoryMode getInventoryMode() { return inventoryMode; }
    public void setInventoryMode(InventoryMode inventoryMode) { this.inventoryMode = inventoryMode; }

    public Integer getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }

    public Integer getMaxBorrowDays() { return maxBorrowDays; }
    public void setMaxBorrowDays(Integer maxBorrowDays) { this.maxBorrowDays = maxBorrowDays; }

    public Boolean getIsReservable() { return isReservable; }
    public void setIsReservable(Boolean reservable) { isReservable = reservable; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean active) { isActive = active; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
