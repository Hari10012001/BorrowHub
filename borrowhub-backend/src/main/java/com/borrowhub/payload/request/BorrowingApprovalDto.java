package com.borrowhub.payload.request;

import jakarta.validation.constraints.NotNull;

public class BorrowingApprovalDto {

    @NotNull(message = "Decision (approved: true/false) is required")
    private Boolean approved;

    private String reviewNotes;

    public BorrowingApprovalDto() {}

    public Boolean getApproved() {
        return approved;
    }

    public void setApproved(Boolean approved) {
        this.approved = approved;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }
}
