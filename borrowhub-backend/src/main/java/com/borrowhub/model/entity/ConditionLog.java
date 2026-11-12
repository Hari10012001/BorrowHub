package com.borrowhub.model.entity;

import com.borrowhub.model.enums.PhysicalCondition;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "condition_logs")
public class ConditionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_unit_id", nullable = false)
    private ResourceUnit resourceUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrowing_record_id")
    private BorrowingRecord borrowingRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logged_by", nullable = false)
    private User loggedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_condition", nullable = false, length = 20)
    private PhysicalCondition previousCondition;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_condition", nullable = false, length = 20)
    private PhysicalCondition newCondition;

    @Column(name = "inspection_notes", columnDefinition = "TEXT", nullable = false)
    private String inspectionNotes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ResourceUnit getResourceUnit() { return resourceUnit; }
    public void setResourceUnit(ResourceUnit resourceUnit) { this.resourceUnit = resourceUnit; }

    public BorrowingRecord getBorrowingRecord() { return borrowingRecord; }
    public void setBorrowingRecord(BorrowingRecord borrowingRecord) { this.borrowingRecord = borrowingRecord; }

    public User getLoggedBy() { return loggedBy; }
    public void setLoggedBy(User loggedBy) { this.loggedBy = loggedBy; }

    public PhysicalCondition getPreviousCondition() { return previousCondition; }
    public void setPreviousCondition(PhysicalCondition previousCondition) { this.previousCondition = previousCondition; }

    public PhysicalCondition getNewCondition() { return newCondition; }
    public void setNewCondition(PhysicalCondition newCondition) { this.newCondition = newCondition; }

    public String getInspectionNotes() { return inspectionNotes; }
    public void setInspectionNotes(String inspectionNotes) { this.inspectionNotes = inspectionNotes; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
