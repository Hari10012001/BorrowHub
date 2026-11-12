package com.borrowhub.repository;

import com.borrowhub.model.entity.ReservationUnit;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReservationUnitRepository extends JpaRepository<ReservationUnit, Long> {

    @Query("SELECT ru.resourceUnit.id FROM ReservationUnit ru WHERE ru.reservation.id = :reservationId")
    List<Long> findMappedUnitIds(@Param("reservationId") Long reservationId);

    @Query("SELECT ru.resourceUnit.assetCode FROM ReservationUnit ru WHERE ru.reservation.id = :reservationId")
    List<String> findAssetCodesByReservationId(@Param("reservationId") Long reservationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ru FROM ReservationUnit ru WHERE ru.reservation.id = :reservationId")
    List<ReservationUnit> findMappingsForReservationForUpdate(@Param("reservationId") Long reservationId);

    // Targeted deletion with row count check (C38)
    @Modifying
    @Query("DELETE FROM ReservationUnit ru WHERE ru.reservation.id = :reservationId AND ru.resourceUnit.id IN :unitIds")
    int deleteSpecificMappedUnits(@Param("reservationId") Long reservationId, @Param("unitIds") List<Long> unitIds);

    @Modifying
    @Query("DELETE FROM ReservationUnit ru WHERE ru.reservation.id = :reservationId")
    int deleteAllByReservationId(@Param("reservationId") Long reservationId);

    // Find unit IDs blocked by active overlapping reservation mappings
    @Query("SELECT ru.resourceUnit.id FROM ReservationUnit ru " +
           "WHERE ru.resourceUnit.resource.id = :resourceId " +
           "AND ru.reservation.status = 'HELD' " +
           "AND ru.allocatedFrom <= :endDate AND ru.allocatedUntil >= :startDate")
    List<Long> findOverlappingAllocatedUnitIds(
            @Param("resourceId") Long resourceId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
