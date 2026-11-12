package com.borrowhub.repository;

import com.borrowhub.model.entity.ConditionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConditionLogRepository extends JpaRepository<ConditionLog, Long> {
    List<ConditionLog> findByResourceUnitIdOrderByCreatedAtDesc(Long resourceUnitId);
}
