package com.borrowhub.model.entity;

import com.borrowhub.model.enums.InventoryMode;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "resources")
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "resource_code", nullable = false, unique = true, length = 50)
    private String resourceCode;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "inventory_mode", nullable = false, length = 20)
    private InventoryMode inventoryMode = InventoryMode.INDIVIDUAL;

    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity = 1;

    @Column(name = "max_borrow_days", nullable = false)
    private int maxBorrowDays = 7;

    @Column(name = "is_reservable", nullable = false)
    private boolean isReservable = true;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public String getResourceCode() { return resourceCode; }
    public void setResourceCode(String resourceCode) { this.resourceCode = resourceCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public InventoryMode getInventoryMode() { return inventoryMode; }
    public void setInventoryMode(InventoryMode inventoryMode) { this.inventoryMode = inventoryMode; }

    public int getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(int totalQuantity) { this.totalQuantity = totalQuantity; }

    public int getMaxBorrowDays() { return maxBorrowDays; }
    public void setMaxBorrowDays(int maxBorrowDays) { this.maxBorrowDays = maxBorrowDays; }

    public boolean isReservable() { return isReservable; }
    public void setReservable(boolean reservable) { isReservable = reservable; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
