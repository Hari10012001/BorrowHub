package com.borrowhub.repository;

import com.borrowhub.model.entity.BorrowingRecord;
import com.borrowhub.model.enums.BorrowingStatus;
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
public interface BorrowingRecordRepository extends JpaRepository<BorrowingRecord, Long> {

    Optional<BorrowingRecord> findByTransactionCode(String transactionCode);
    Optional<BorrowingRecord> findByIdempotencyKey(String idempotencyKey);
    List<BorrowingRecord> findByUserIdOrderByCreatedAtDesc(Long userId);
    Page<BorrowingRecord> findByStatus(BorrowingStatus status, Pageable pageable);
    Page<BorrowingRecord> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Level 4 Lock (DAG Hierarchy)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BorrowingRecord b WHERE b.id = :id")
    Optional<BorrowingRecord> findByIdForUpdate(@Param("id") Long id);

    // Sum outstanding quantity across active loans (C7: Includes OVERDUE and PARTIALLY_RETURNED)
    @Query("SELECT COALESCE(SUM(b.quantityIssued - b.quantityReturned - b.quantityLost), 0) " +
           "FROM BorrowingRecord b WHERE b.resource.id = :resourceId " +
           "AND b.status IN ('ACTIVE', 'PARTIALLY_RETURNED', 'RETURN_PENDING', 'OVERDUE')")
    int sumCommittedOutstandingQuantity(@Param("resourceId") Long resourceId);

    // Find all active records where due date has passed (for daily midnight overdue job)
    @Query(value = "SELECT * FROM borrowing_records WHERE status IN ('ACTIVE', 'PARTIALLY_RETURNED') AND due_date < CURRENT_DATE()", nativeQuery = true)
    List<BorrowingRecord> findRecordsBecomingOverdue();

    // Check if an individual asset is currently in active custody
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN TRUE ELSE FALSE END FROM BorrowingRecord b " +
           "WHERE b.resourceUnit.id = :unitId " +
           "AND b.status IN ('ACTIVE', 'PARTIALLY_RETURNED', 'RETURN_PENDING', 'OVERDUE')")
    boolean isUnitInActiveCustody(@Param("unitId") Long unitId);

    long countByUserIdAndStatusIn(Long userId, List<BorrowingStatus> statuses);
}
