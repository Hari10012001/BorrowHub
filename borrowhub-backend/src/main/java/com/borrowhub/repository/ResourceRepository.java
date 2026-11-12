package com.borrowhub.repository;

import com.borrowhub.model.entity.Resource;
import com.borrowhub.model.enums.InventoryMode;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {

    Optional<Resource> findByResourceCode(String resourceCode);

    // Level 1 Lock (DAG Hierarchy)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Resource r WHERE r.id = :id")
    Optional<Resource> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT r FROM Resource r WHERE " +
           "(:categoryId IS NULL OR r.category.id = :categoryId) AND " +
           "(:mode IS NULL OR r.inventoryMode = :mode) AND " +
           "(:activeOnly IS NULL OR r.isActive = :activeOnly) AND " +
           "(:search IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(r.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Resource> searchResources(
            @Param("categoryId") Long categoryId,
            @Param("mode") InventoryMode mode,
            @Param("activeOnly") Boolean activeOnly,
            @Param("search") String search,
            Pageable pageable
    );
}
