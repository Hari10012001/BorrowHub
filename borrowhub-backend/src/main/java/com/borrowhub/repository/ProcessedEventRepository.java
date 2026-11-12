package com.borrowhub.repository;

import com.borrowhub.model.entity.ProcessedEvent;
import com.borrowhub.model.entity.ProcessedEventId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, ProcessedEventId> {

    @Query("SELECT CASE WHEN COUNT(pe) > 0 THEN TRUE ELSE FALSE END FROM ProcessedEvent pe " +
           "WHERE pe.id.eventId = :eventId AND pe.id.consumerName = :consumerName")
    boolean isEventAlreadyProcessed(@Param("eventId") Long eventId, @Param("consumerName") String consumerName);
}
