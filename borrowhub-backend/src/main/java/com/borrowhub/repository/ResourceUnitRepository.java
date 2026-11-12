package com.borrowhub.repository;

import com.borrowhub.model.entity.ResourceUnit;
import com.borrowhub.model.enums.UnitStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceUnitRepository extends JpaRepository<ResourceUnit, Long> {

    Optional<ResourceUnit> findByAssetCode(String assetCode);
    Optional<ResourceUnit> findByQrCodeData(String qrCodeData);
    List<ResourceUnit> findByResourceId(Long resourceId);
    List<ResourceUnit> findByResourceIdAndStatus(Long resourceId, UnitStatus status);
    int countByResourceId(Long resourceId);

    // Level 2 Lock (Strict Ascending Primary Key Order - C43)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM ResourceUnit u WHERE u.id IN :ids ORDER BY u.id ASC")
    List<ResourceUnit> findUnitsByIdsForUpdate(@Param("ids") List<Long> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM ResourceUnit u WHERE u.id = :id")
    Optional<ResourceUnit> findByIdForUpdate(@Param("id") Long id);
}
