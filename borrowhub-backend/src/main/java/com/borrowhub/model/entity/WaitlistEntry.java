package com.borrowhub.model.entity;

import com.borrowhub.model.enums.WaitlistStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "waitlist_entries")
public class WaitlistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "priority_tier", nullable = false)
    private int priorityTier = 2; // 1 = Faculty, 2 = Student

    @Column(name = "queue_position", nullable = false)
    private int queuePosition;

    @Column(name = "requested_start", nullable = false)
    private LocalDate requestedStart;

    @Column(name = "requested_end", nullable = false)
    private LocalDate requestedEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WaitlistStatus status = WaitlistStatus.WAITING;

    @Column(name = "notified_at")
    private Instant notifiedAt;

    @Column(name = "claim_deadline")
    private Instant claimDeadline;

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

    public Resource getResource() { return resource; }
    public void setResource(Resource resource) { this.resource = resource; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public int getPriorityTier() { return priorityTier; }
    public void setPriorityTier(int priorityTier) { this.priorityTier = priorityTier; }

    public int getQueuePosition() { return queuePosition; }
    public void setQueuePosition(int queuePosition) { this.queuePosition = queuePosition; }

    public LocalDate getRequestedStart() { return requestedStart; }
    public void setRequestedStart(LocalDate requestedStart) { this.requestedStart = requestedStart; }

    public LocalDate getRequestedEnd() { return requestedEnd; }
    public void setRequestedEnd(LocalDate requestedEnd) { this.requestedEnd = requestedEnd; }

    public WaitlistStatus getStatus() { return status; }
    public void setStatus(WaitlistStatus status) { this.status = status; }

    public Instant getNotifiedAt() { return notifiedAt; }
    public void setNotifiedAt(Instant notifiedAt) { this.notifiedAt = notifiedAt; }

    public Instant getClaimDeadline() { return claimDeadline; }
    public void setClaimDeadline(Instant claimDeadline) { this.claimDeadline = claimDeadline; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
