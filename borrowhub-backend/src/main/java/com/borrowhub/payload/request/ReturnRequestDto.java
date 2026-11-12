package com.borrowhub.payload.request;

import com.borrowhub.model.enums.PhysicalCondition;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class ReturnRequestDto {

    @NotNull(message = "Borrowing record ID is required")
    private Long borrowingRecordId;

    @NotNull(message = "Quantity returned is required")
    @Min(value = 1, message = "Quantity returned must be at least 1")
    private Integer quantityReturned;

    private List<Long> returnedUnitIds;
    private List<String> returnedAssetTags;

    private PhysicalCondition returnCondition = PhysicalCondition.GOOD;
    private String damageNotes;
    private String notes;

    public ReturnRequestDto() {}

    public Long getBorrowingRecordId() {
        return borrowingRecordId;
    }

    public void setBorrowingRecordId(Long borrowingRecordId) {
        this.borrowingRecordId = borrowingRecordId;
    }

    public Integer getQuantityReturned() {
        return quantityReturned;
    }

    public void setQuantityReturned(Integer quantityReturned) {
        this.quantityReturned = quantityReturned;
    }

    public List<Long> getReturnedUnitIds() {
        return returnedUnitIds;
    }

    public void setReturnedUnitIds(List<Long> returnedUnitIds) {
        this.returnedUnitIds = returnedUnitIds;
    }

    public List<String> getReturnedAssetTags() {
        return returnedAssetTags;
    }

    public void setReturnedAssetTags(List<String> returnedAssetTags) {
        this.returnedAssetTags = returnedAssetTags;
    }

    public PhysicalCondition getReturnCondition() {
        return returnCondition;
    }

    public void setReturnCondition(PhysicalCondition returnCondition) {
        this.returnCondition = returnCondition;
    }

    public String getDamageNotes() {
        return damageNotes;
    }

    public void setDamageNotes(String damageNotes) {
        this.damageNotes = damageNotes;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
