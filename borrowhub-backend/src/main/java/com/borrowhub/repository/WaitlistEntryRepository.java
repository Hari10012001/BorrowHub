package com.borrowhub.repository;

import com.borrowhub.model.entity.WaitlistEntry;
import com.borrowhub.model.enums.WaitlistStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, Long> {

    List<WaitlistEntry> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<WaitlistEntry> findByResourceIdAndStatusOrderByPriorityTierAscQueuePositionAsc(Long resourceId, WaitlistStatus status);

    // Level 5 Lock (DAG Hierarchy)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM WaitlistEntry w WHERE w.id = :id")
    Optional<WaitlistEntry> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT COALESCE(MAX(w.queuePosition), 0) FROM WaitlistEntry w WHERE w.resource.id = :resourceId")
    int findMaxQueuePosition(@Param("resourceId") Long resourceId);

    // Find expired NOTIFIED waitlist entries whose claim deadline has passed
    @Query(value = "SELECT * FROM waitlist_entries WHERE status = 'NOTIFIED' AND claim_deadline < NOW(6)", nativeQuery = true)
    List<WaitlistEntry> findExpiredNotifiedEntries();
}
