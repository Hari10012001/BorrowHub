package com.borrowhub.model.entity;

import com.borrowhub.model.enums.PhysicalCondition;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "return_transactions")
public class ReturnTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrowing_record_id", nullable = false)
    private BorrowingRecord borrowingRecord;

    @Column(name = "returned_quantity", nullable = false)
    private int returnedQuantity;

    @Column(name = "damaged_quantity", nullable = false)
    private int damagedQuantity = 0;

    @Column(name = "accepted_quantity", nullable = false)
    private int acceptedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_assessed", nullable = false, length = 20)
    private PhysicalCondition conditionAssessed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "received_by", nullable = false)
    private User receivedBy;

    @Column(name = "return_notes", columnDefinition = "TEXT")
    private String returnNotes;

    @Column(name = "returned_at", nullable = false, updatable = false)
    private Instant returnedAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        if (returnedAt == null) returnedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BorrowingRecord getBorrowingRecord() { return borrowingRecord; }
    public void setBorrowingRecord(BorrowingRecord borrowingRecord) { this.borrowingRecord = borrowingRecord; }

    public int getReturnedQuantity() { return returnedQuantity; }
    public void setReturnedQuantity(int returnedQuantity) { this.returnedQuantity = returnedQuantity; }

    public int getDamagedQuantity() { return damagedQuantity; }
    public void setDamagedQuantity(int damagedQuantity) { this.damagedQuantity = damagedQuantity; }

    public int getAcceptedQuantity() { return acceptedQuantity; }
    public void setAcceptedQuantity(int acceptedQuantity) { this.acceptedQuantity = acceptedQuantity; }

    public PhysicalCondition getConditionAssessed() { return conditionAssessed; }
    public void setConditionAssessed(PhysicalCondition conditionAssessed) { this.conditionAssessed = conditionAssessed; }

    public User getReceivedBy() { return receivedBy; }
    public void setReceivedBy(User receivedBy) { this.receivedBy = receivedBy; }

    public String getReturnNotes() { return returnNotes; }
    public void setReturnNotes(String returnNotes) { this.returnNotes = returnNotes; }

    public Instant getReturnedAt() { return returnedAt; }
    public void setReturnedAt(Instant returnedAt) { this.returnedAt = returnedAt; }
}
