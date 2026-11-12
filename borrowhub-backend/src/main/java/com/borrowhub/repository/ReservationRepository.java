package com.borrowhub.repository;

import com.borrowhub.model.entity.Reservation;
import com.borrowhub.model.enums.ReservationStatus;
import com.borrowhub.repository.projection.ReservationMetaProjection;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByReservationNumber(String reservationNumber);

    List<Reservation> findByUserIdOrderByCreatedAtDesc(Long userId);

    Page<Reservation> findByStatus(ReservationStatus status, Pageable pageable);

    // Non-locking metadata discovery projection (C43 Step 1)
    @Query("SELECT r.id AS id, r.reservationNumber AS reservationNumber, r.resource.id AS resourceId, r.quantity AS quantity " +
           "FROM Reservation r WHERE r.id = :id")
    Optional<ReservationMetaProjection> findMetaById(@Param("id") Long id);

    // Level 3 Lock (DAG Hierarchy)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reservation r WHERE r.id = :id")
    Optional<Reservation> findByIdForUpdate(@Param("id") Long id);

    // Find active or held overlapping reservations for availability calculations (C7/C12/C14)
    @Query("SELECT r FROM Reservation r WHERE r.resource.id = :resourceId " +
           "AND r.status = 'HELD' " +
           "AND r.startDate <= :endDate AND r.endDate >= :startDate")
    List<Reservation> findOverlappingHeldReservations(
            @Param("resourceId") Long resourceId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // Find expired HELD reservations where claim deadline has passed
    @Query(value = "SELECT * FROM reservations WHERE status = 'HELD' AND claim_deadline < NOW(6)", nativeQuery = true)
    List<Reservation> findExpiredHeldReservations();

    long countByUserIdAndStatusIn(Long userId, List<ReservationStatus> statuses);

    @Query("SELECT COALESCE(SUM(r.quantity), 0) FROM Reservation r WHERE r.resource.id = :resourceId AND r.status = 'HELD'")
    int sumHeldQuantity(@Param("resourceId") Long resourceId);

    @Query("SELECT CASE WHEN COUNT(ru) > 0 THEN TRUE ELSE FALSE END FROM ReservationUnit ru " +
           "WHERE ru.resourceUnit.id = :unitId AND ru.reservation.status = 'HELD'")
    boolean isUnitInActiveReservation(@Param("unitId") Long unitId);
}
