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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceUnitTest {

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private ResourceUnitRepository resourceUnitRepository;

    @Mock
    private BorrowingRecordRepository borrowingRecordRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private AvailabilityService availabilityService;

    private Resource bulkResource;
    private Resource individualResource;

    @BeforeEach
    void setUp() {
        bulkResource = new Resource();
        bulkResource.setId(10L);
        bulkResource.setName("HDMI Cables");
        bulkResource.setInventoryMode(InventoryMode.QUANTITY);
        bulkResource.setTotalQuantity(50);
        bulkResource.setActive(true);

        individualResource = new Resource();
        individualResource.setId(20L);
        individualResource.setName("MacBook Pro");
        individualResource.setInventoryMode(InventoryMode.INDIVIDUAL);
        individualResource.setTotalQuantity(5);
        individualResource.setActive(true);
    }

    @Test
    @DisplayName("Should return 0 available quantity when resource is not found or inactive")
    void testUnknownOrInactiveResourceReturnsZero() {
        when(resourceRepository.findById(999L)).thenReturn(Optional.empty());
        int qty = availabilityService.getAvailableQuantity(999L);
        assertThat(qty).isEqualTo(0);

        bulkResource.setActive(false);
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(bulkResource));
        int inactiveQty = availabilityService.getAvailableQuantity(10L);
        assertThat(inactiveQty).isEqualTo(0);
    }

    @Test
    @DisplayName("C14: Should compute QUANTITY resource availability: total - borrowed - held")
    void testBulkResourceAvailableQuantity() {
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(bulkResource));
        when(borrowingRecordRepository.sumCommittedOutstandingQuantity(10L)).thenReturn(15);
        when(reservationRepository.sumHeldQuantity(10L)).thenReturn(10);

        int available = availabilityService.getAvailableQuantity(10L);
        // 50 total - 15 borrowed - 10 held = 25
        assertThat(available).isEqualTo(25);
    }

    @Test
    @DisplayName("Should prevent negative available quantity when overcommitted")
    void testBulkResourceZeroBound() {
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(bulkResource));
        when(borrowingRecordRepository.sumCommittedOutstandingQuantity(10L)).thenReturn(40);
        when(reservationRepository.sumHeldQuantity(10L)).thenReturn(25);

        int available = availabilityService.getAvailableQuantity(10L);
        // 50 - 40 - 25 = -15 -> bound to 0
        assertThat(available).isEqualTo(0);
    }

    @Test
    @DisplayName("C12: Should compute INDIVIDUAL resource availability from eligible units only")
    void testIndividualResourceAvailableUnits() {
        when(resourceRepository.findById(20L)).thenReturn(Optional.of(individualResource));

        ResourceUnit unit1 = new ResourceUnit();
        unit1.setId(101L);
        unit1.setStatus(UnitStatus.AVAILABLE);
        unit1.setPhysicalCondition(PhysicalCondition.NEW);

        ResourceUnit unit2 = new ResourceUnit();
        unit2.setId(102L);
        unit2.setStatus(UnitStatus.AVAILABLE);
        unit2.setPhysicalCondition(PhysicalCondition.GOOD);

        ResourceUnit damagedUnit = new ResourceUnit();
        damagedUnit.setId(103L);
        damagedUnit.setStatus(UnitStatus.AVAILABLE);
        damagedUnit.setPhysicalCondition(PhysicalCondition.DAMAGED);

        when(resourceUnitRepository.findByResourceIdAndStatus(20L, UnitStatus.AVAILABLE))
                .thenReturn(List.of(unit1, unit2, damagedUnit));

        List<ResourceUnit> units = availabilityService.getAvailableUnits(20L);
        // Unit 3 is DAMAGED, so it should be filtered out by AvailabilityService
        assertThat(units).hasSize(2);
        assertThat(units).extracting(ResourceUnit::getId).containsExactly(101L, 102L);

        int availableQty = availabilityService.getAvailableQuantity(20L);
        assertThat(availableQty).isEqualTo(2);
    }
}
