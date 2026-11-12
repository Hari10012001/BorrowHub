package com.borrowhub.repository;

import com.borrowhub.model.entity.MaintenanceRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, Long> {
    List<MaintenanceRecord> findByResourceUnitIdOrderByCreatedAtDesc(Long resourceUnitId);
    Page<MaintenanceRecord> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
