package com.borrowhub.model.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "reservation_units", uniqueConstraints = {
    @UniqueConstraint(name = "uq_reservation_unit_link", columnNames = {"reservation_id", "resource_unit_id"})
})
public class ReservationUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_unit_id", nullable = false)
    private ResourceUnit resourceUnit;

    @Column(name = "allocated_from", nullable = false)
    private LocalDate allocatedFrom;

    @Column(name = "allocated_until", nullable = false)
    private LocalDate allocatedUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Reservation getReservation() { return reservation; }
    public void setReservation(Reservation reservation) { this.reservation = reservation; }

    public ResourceUnit getResourceUnit() { return resourceUnit; }
    public void setResourceUnit(ResourceUnit resourceUnit) { this.resourceUnit = resourceUnit; }

    public LocalDate getAllocatedFrom() { return allocatedFrom; }
    public void setAllocatedFrom(LocalDate allocatedFrom) { this.allocatedFrom = allocatedFrom; }

    public LocalDate getAllocatedUntil() { return allocatedUntil; }
    public void setAllocatedUntil(LocalDate allocatedUntil) { this.allocatedUntil = allocatedUntil; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
