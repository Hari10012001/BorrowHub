package com.borrowhub.payload.response;

import com.borrowhub.model.enums.PhysicalCondition;
import com.borrowhub.model.enums.UnitStatus;

import java.time.Instant;
import java.time.LocalDate;

public class ResourceUnitResponse {

    private Long id;
    private Long resourceId;
    private String resourceName;
    private String assetCode;
    private String qrCodeData;
    private String serialNumber;
    private String brandModel;
    private PhysicalCondition physicalCondition;
    private UnitStatus status;
    private String storageLocation;
    private LocalDate acquisitionDate;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;

    public ResourceUnitResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }

    public String getResourceName() { return resourceName; }
    public void setResourceName(String resourceName) { this.resourceName = resourceName; }

    public String getAssetCode() { return assetCode; }
    public void setAssetCode(String assetCode) { this.assetCode = assetCode; }

    public String getQrCodeData() { return qrCodeData; }
    public void setQrCodeData(String qrCodeData) { this.qrCodeData = qrCodeData; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public String getBrandModel() { return brandModel; }
    public void setBrandModel(String brandModel) { this.brandModel = brandModel; }

    public PhysicalCondition getPhysicalCondition() { return physicalCondition; }
    public void setPhysicalCondition(PhysicalCondition physicalCondition) { this.physicalCondition = physicalCondition; }

    public UnitStatus getStatus() { return status; }
    public void setStatus(UnitStatus status) { this.status = status; }

    public String getStorageLocation() { return storageLocation; }
    public void setStorageLocation(String storageLocation) { this.storageLocation = storageLocation; }

    public LocalDate getAcquisitionDate() { return acquisitionDate; }
    public void setAcquisitionDate(LocalDate acquisitionDate) { this.acquisitionDate = acquisitionDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
