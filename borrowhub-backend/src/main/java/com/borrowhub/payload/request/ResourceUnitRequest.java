package com.borrowhub.payload.request;

import com.borrowhub.model.enums.PhysicalCondition;
import com.borrowhub.model.enums.UnitStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ResourceUnitRequest {

    @NotNull(message = "Resource ID is required")
    private Long resourceId;

    @NotBlank(message = "Asset code is required")
    @Size(max = 50, message = "Asset code must not exceed 50 characters")
    private String assetCode;

    @Size(max = 100, message = "Serial number must not exceed 100 characters")
    private String serialNumber;

    @Size(max = 150, message = "Brand/Model must not exceed 150 characters")
    private String brandModel;

    private PhysicalCondition physicalCondition = PhysicalCondition.GOOD;
    private UnitStatus status = UnitStatus.AVAILABLE;

    @NotBlank(message = "Storage location is required")
    @Size(max = 100, message = "Storage location must not exceed 100 characters")
    private String storageLocation;

    private String notes;

    public ResourceUnitRequest() {}

    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }

    public String getAssetCode() { return assetCode; }
    public void setAssetCode(String assetCode) { this.assetCode = assetCode; }

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

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
