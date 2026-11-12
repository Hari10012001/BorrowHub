package com.borrowhub.service;

import com.borrowhub.model.entity.Resource;
import com.borrowhub.model.entity.ResourceUnit;
import com.borrowhub.model.enums.InventoryMode;
import com.borrowhub.model.enums.PhysicalCondition;
import com.borrowhub.model.enums.UnitStatus;
import com.borrowhub.repository.BorrowingRecordRepository;
import com.borrowhub.repository.ReservationRepository;
import com.borrowhub.repository.ResourceRepository;
import com.borrowhub.repository.ResourceUnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Authoritative availability calculation service.
 * Enforces C12 & C14:
 * - ACTIVE, PARTIALLY_RETURNED, OVERDUE, and RETURN_PENDING records retain their
 *   outstanding custody window against available inventory.
 * - HELD reservations retain their reserved quantity.
 */
@Service
public class AvailabilityService {

    private final ResourceRepository resourceRepository;
    private final ResourceUnitRepository resourceUnitRepository;
    private final BorrowingRecordRepository borrowingRecordRepository;
    private final ReservationRepository reservationRepository;

    public AvailabilityService(ResourceRepository resourceRepository,
                               ResourceUnitRepository resourceUnitRepository,
                               BorrowingRecordRepository borrowingRecordRepository,
                               ReservationRepository reservationRepository) {
        this.resourceRepository = resourceRepository;
        this.resourceUnitRepository = resourceUnitRepository;
        this.borrowingRecordRepository = borrowingRecordRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional(readOnly = true)
    public int getAvailableQuantity(Long resourceId) {
        Resource resource = resourceRepository.findById(resourceId).orElse(null);
        if (resource == null || !resource.isActive()) {
            return 0;
        }

        if (resource.getInventoryMode() == InventoryMode.INDIVIDUAL) {
            return getAvailableUnits(resourceId).size();
        } else {
            // QUANTITY mode
            int total = resource.getTotalQuantity();

            // Sum outstanding quantities of active borrowings (C7: includes OVERDUE and PARTIALLY_RETURNED)
            int borrowed = borrowingRecordRepository.sumCommittedOutstandingQuantity(resourceId);

            // Sum active HELD reservations
            int held = reservationRepository.sumHeldQuantity(resourceId);

            int available = total - borrowed - held;
            return Math.max(0, available);
        }
    }

    @Transactional(readOnly = true)
    public List<ResourceUnit> getAvailableUnits(Long resourceId) {
        // Query units with status AVAILABLE
        List<ResourceUnit> units = resourceUnitRepository.findByResourceIdAndStatus(resourceId, UnitStatus.AVAILABLE);

        // Filter out any unit currently tied to an active borrowing or held reservation (C12)
        return units.stream()
                .filter(u -> u.getPhysicalCondition() != PhysicalCondition.DAMAGED && u.getPhysicalCondition() != PhysicalCondition.LOST)
                .filter(u -> !borrowingRecordRepository.isUnitInActiveCustody(u.getId()))
                .filter(u -> !reservationRepository.isUnitInActiveReservation(u.getId()))
                .toList();
    }
}
