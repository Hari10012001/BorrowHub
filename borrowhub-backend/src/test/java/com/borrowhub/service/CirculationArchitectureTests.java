package com.borrowhub.service;

import com.borrowhub.model.entity.*;
import com.borrowhub.model.enums.*;
import com.borrowhub.payload.request.CheckoutRequestDto;
import com.borrowhub.payload.response.CheckoutResponseDto;
import com.borrowhub.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
public class CirculationArchitectureTests {

    @Autowired
    private AvailabilityService availabilityService;

    @Autowired
    private CirculationTransactionalService circulationTransactionalService;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private ResourceUnitRepository resourceUnitRepository;

    @Autowired
    private BorrowingRequestRepository borrowingRequestRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ReservationUnitRepository reservationUnitRepository;

    @Autowired
    private BorrowingRecordRepository borrowingRecordRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CirculationRetryOrchestrator circulationRetryOrchestrator;

    private Reservation createTestReservation(User user, Resource resource, int quantity, Instant claimDeadline) {
        BorrowingRequest req = new BorrowingRequest();
        req.setRequestNumber("REQ-" + UUID.randomUUID().toString().substring(0, 8));
        req.setUser(user);
        req.setResource(resource);
        req.setQuantity(quantity);
        req.setRequestedStart(LocalDate.now());
        req.setRequestedEnd(LocalDate.now().plusDays(7));
        req.setPurpose("Academic Research and Lab Project Testing");
        req.setStatus(RequestStatus.APPROVED);
        req = borrowingRequestRepository.save(req);

        Reservation res = new Reservation();
        res.setReservationNumber("RES-" + UUID.randomUUID().toString().substring(0, 8));
        res.setRequest(req);
        res.setUser(user);
        res.setResource(resource);
        res.setQuantity(quantity);
        res.setStartDate(LocalDate.now());
        res.setEndDate(LocalDate.now().plusDays(7));
        res.setClaimDeadline(claimDeadline);
        res.setStatus(ReservationStatus.HELD);
        return reservationRepository.save(res);
    }

    @Test
    @DisplayName("C12: INDIVIDUAL asset in active custody window (ACTIVE, OVERDUE) blocks unit availability")
    void testC12IndividualPartialReturnCustodyWindow() {
        // Retrieve seeded INDIVIDUAL resource (Dell XPS 15)
        Resource laptop = resourceRepository.findAll().stream()
                .filter(r -> r.getInventoryMode() == InventoryMode.INDIVIDUAL)
                .findFirst()
                .orElseThrow();

        // Create an isolated fresh test unit to guarantee zero prior test contamination
        ResourceUnit testUnit = new ResourceUnit();
        testUnit.setResource(laptop);
        testUnit.setAssetCode("TEST-UNIT-" + UUID.randomUUID().toString().substring(0, 8));
        testUnit.setSerialNumber("SN-" + UUID.randomUUID().toString().substring(0, 8));
        testUnit.setQrCodeData("QR-" + UUID.randomUUID());
        testUnit.setStorageLocation("Lab Cabinet A-1");
        testUnit.setStatus(UnitStatus.AVAILABLE);
        testUnit.setPhysicalCondition(PhysicalCondition.NEW);
        testUnit = resourceUnitRepository.save(testUnit);
        final Long targetUnitId = testUnit.getId();

        int initialAvailable = availabilityService.getAvailableQuantity(laptop.getId());
        assertTrue(initialAvailable >= 1, "Initial available quantity must include new test unit");

        User student = userRepository.findByUsername("student1").orElseThrow();
        User admin = userRepository.findByUsername("admin").orElseThrow();

        // Create a test reservation for this unit
        Reservation res = createTestReservation(student, laptop, 1, Instant.now().plusSeconds(3600));

        // Checkout the unit
        CheckoutRequestDto checkoutDto = new CheckoutRequestDto();
        checkoutDto.setReservationId(res.getId());
        checkoutDto.setUserId(student.getId());
        checkoutDto.setResourceId(laptop.getId());
        checkoutDto.setUnitIds(List.of(targetUnitId));
        checkoutDto.setDueDate(LocalDate.now().plusDays(7));
        checkoutDto.setQuantity(1);

        CheckoutResponseDto checkoutResponse = circulationTransactionalService.executeCheckout(checkoutDto, admin.getId());
        assertNotNull(checkoutResponse);

        // Verify available quantity decreased by 1
        int afterCheckoutAvailable = availabilityService.getAvailableQuantity(laptop.getId());
        assertEquals(initialAvailable - 1, afterCheckoutAvailable);

        // Verify unit is in active custody
        assertTrue(borrowingRecordRepository.isUnitInActiveCustody(targetUnitId));

        // Transition borrowing record to OVERDUE status (retains active custody under C12)
        jdbcTemplate.update(
                "UPDATE borrowing_records SET status = 'OVERDUE' WHERE id = ?",
                checkoutResponse.getBorrowingRecordId()
        );

        // Under C12, active custody records retain custody and block unit from re-borrowing
        int duringOverdueCustody = availabilityService.getAvailableQuantity(laptop.getId());
        assertEquals(initialAvailable - 1, duringOverdueCustody, "OVERDUE custody record must retain custody and block unit");

        List<ResourceUnit> availableUnits = availabilityService.getAvailableUnits(laptop.getId());
        boolean targetUnitStillAvailable = availableUnits.stream().anyMatch(u -> u.getId().equals(targetUnitId));
        assertFalse(targetUnitStillAvailable, "Unit in active custody must be excluded from available units");
    }

    @Test
    @DisplayName("C13: MySQL check constraint chk_br_terminal_consistency strictly enforces terminal states")
    void testC13TerminalConsistencyEnforcement() {
        User student = userRepository.findByUsername("student1").orElseThrow();
        User admin = userRepository.findByUsername("admin").orElseThrow();
        Resource bulkResource = resourceRepository.findAll().stream()
                .filter(r -> r.getInventoryMode() == InventoryMode.QUANTITY)
                .findFirst()
                .orElseThrow();

        Reservation res = createTestReservation(student, bulkResource, 2, Instant.now().plusSeconds(3600));

        final Long resId = res.getId();
        final Long studId = student.getId();
        final Long resrcId = bulkResource.getId();
        final Long adminId = admin.getId();

        // 1. Valid pure physical return: status = RETURNED, actual_return_date NOT NULL, closed_at NULL
        String validReturnedTx = "TX-TERM-RET-" + UUID.randomUUID().toString().substring(0, 8);
        assertDoesNotThrow(() -> {
            jdbcTemplate.update(
                    "INSERT INTO borrowing_records (transaction_code, reservation_id, user_id, resource_id, quantity_issued, " +
                    "quantity_returned, quantity_damaged, quantity_lost, status, actual_return_date, closed_at, due_date, issued_by) " +
                    "VALUES (?, ?, ?, ?, 2, 2, 0, 0, 'RETURNED', NOW(), NULL, DATE_ADD(CURRENT_DATE(), INTERVAL 7 DAY), ?)",
                    validReturnedTx, resId, studId, resrcId, adminId
            );
        });

        // 2. Invalid RETURNED: actual_return_date IS NULL (must violate constraint)
        String invalidReturnedTx = "TX-TERM-INV-" + UUID.randomUUID().toString().substring(0, 8);
        Exception ex = assertThrows(org.springframework.dao.DataAccessException.class, () -> {
            jdbcTemplate.update(
                    "INSERT INTO borrowing_records (transaction_code, reservation_id, user_id, resource_id, quantity_issued, " +
                    "quantity_returned, quantity_damaged, quantity_lost, status, actual_return_date, closed_at, due_date, issued_by) " +
                    "VALUES (?, ?, ?, ?, 2, 2, 0, 0, 'RETURNED', NULL, NULL, DATE_ADD(CURRENT_DATE(), INTERVAL 7 DAY), ?)",
                    invalidReturnedTx, resId, studId, resrcId, adminId
            );
        });
        assertTrue(ex.getMessage().contains("chk_br_terminal_consistency"), "Constraint chk_br_terminal_consistency must be violated");

        // 3. Valid Pure-loss CLOSED: status = CLOSED, returned = 0, lost = issued, actual_return_date NULL, closed_at NOT NULL
        String validPureLossTx = "TX-TERM-LOSS-" + UUID.randomUUID().toString().substring(0, 8);
        assertDoesNotThrow(() -> {
            jdbcTemplate.update(
                    "INSERT INTO borrowing_records (transaction_code, reservation_id, user_id, resource_id, quantity_issued, " +
                    "quantity_returned, quantity_damaged, quantity_lost, status, actual_return_date, closed_at, due_date, issued_by) " +
                    "VALUES (?, ?, ?, ?, 2, 0, 0, 2, 'CLOSED', NULL, NOW(6), DATE_ADD(CURRENT_DATE(), INTERVAL 7 DAY), ?)",
                    validPureLossTx, resId, studId, resrcId, adminId
            );
        });

        // 4. Valid Mixed-outcome CLOSED: status = CLOSED, returned = 1, lost = 1, actual_return_date NOT NULL, closed_at NOT NULL
        String validMixedTx = "TX-TERM-MIX-" + UUID.randomUUID().toString().substring(0, 8);
        assertDoesNotThrow(() -> {
            jdbcTemplate.update(
                    "INSERT INTO borrowing_records (transaction_code, reservation_id, user_id, resource_id, quantity_issued, " +
                    "quantity_returned, quantity_damaged, quantity_lost, status, actual_return_date, closed_at, due_date, issued_by) " +
                    "VALUES (?, ?, ?, ?, 2, 1, 0, 1, 'CLOSED', NOW(), NOW(6), DATE_ADD(CURRENT_DATE(), INTERVAL 7 DAY), ?)",
                    validMixedTx, resId, studId, resrcId, adminId
            );
        });
    }

    @Test
    @DisplayName("C17: STORED generated column quantity_outstanding updates dynamically in MySQL and syncs to JPA")
    void testC17StoredGeneratedColumnAndJpaSync() {
        User student = userRepository.findByUsername("student1").orElseThrow();
        User admin = userRepository.findByUsername("admin").orElseThrow();
        Resource bulkResource = resourceRepository.findAll().stream()
                .filter(r -> r.getInventoryMode() == InventoryMode.QUANTITY)
                .findFirst()
                .orElseThrow();

        Reservation res = createTestReservation(student, bulkResource, 5, Instant.now().plusSeconds(3600));

        String txCode = "TX-C17-" + UUID.randomUUID().toString().substring(0, 8);
        jdbcTemplate.update(
                "INSERT INTO borrowing_records (transaction_code, reservation_id, user_id, resource_id, quantity_issued, " +
                "quantity_returned, quantity_damaged, quantity_lost, status, due_date, issued_by) " +
                "VALUES (?, ?, ?, ?, 5, 2, 0, 1, 'ACTIVE', DATE_ADD(CURRENT_DATE(), INTERVAL 7 DAY), ?)",
                txCode, res.getId(), student.getId(), bulkResource.getId(), admin.getId()
        );

        Long recordId = jdbcTemplate.queryForObject(
                "SELECT id FROM borrowing_records WHERE transaction_code = ?", Long.class, txCode);
        assertNotNull(recordId);

        // Fetch through JPA repository
        BorrowingRecord record = borrowingRecordRepository.findById(recordId).orElseThrow();
        // MySQL stored generated column: 5 - 2 - 1 = 2
        assertEquals(2, record.getQuantityOutstanding(), "quantity_outstanding must be computed by MySQL as 5 - 2 - 1 = 2");

        // Update returned quantity via SQL
        jdbcTemplate.update(
                "UPDATE borrowing_records SET quantity_returned = 3 WHERE id = ?", recordId);

        // Re-query from database
        Integer outstanding = jdbcTemplate.queryForObject(
                "SELECT quantity_outstanding FROM borrowing_records WHERE id = ?", Integer.class, recordId);
        assertEquals(1, outstanding, "Stored generated column must reflect 5 - 3 - 1 = 1");
    }

    @Test
    @DisplayName("C21: Reservation expiry commits durably in dedicated transaction")
    void testC21ReservationExpiryDurability() {
        User student = userRepository.findByUsername("student1").orElseThrow();
        Resource bulkResource = resourceRepository.findAll().stream()
                .filter(r -> r.getInventoryMode() == InventoryMode.QUANTITY)
                .findFirst()
                .orElseThrow();

        Reservation res = createTestReservation(student, bulkResource, 1, Instant.now().minusSeconds(10));
        final Long reservationId = res.getId();

        // Expire durably
        reservationService.expireReservationDurable(reservationId);

        // Re-read from database
        Reservation updatedRes = reservationRepository.findById(reservationId).orElseThrow();
        assertEquals(ReservationStatus.EXPIRED, updatedRes.getStatus(), "Reservation must be durably marked EXPIRED");
        assertNotNull(updatedRes.getCancellationReason());
    }

    @Test
    @DisplayName("C30/C38: Partial checkout releases remainder and emits STOCK_REPLENISHED outbox event")
    void testC30AndC38PartialCheckoutRemainderManagement() {
        User student = userRepository.findByUsername("student1").orElseThrow();
        User admin = userRepository.findByUsername("admin").orElseThrow();
        Resource bulkResource = resourceRepository.findAll().stream()
                .filter(r -> r.getInventoryMode() == InventoryMode.QUANTITY)
                .findFirst()
                .orElseThrow();

        // Hold 3 units
        Reservation res = createTestReservation(student, bulkResource, 3, Instant.now().plusSeconds(3600));

        // Checkout only 2 units (partial checkout: 2 of 3)
        CheckoutRequestDto dto = new CheckoutRequestDto();
        dto.setReservationId(res.getId());
        dto.setUserId(student.getId());
        dto.setResourceId(bulkResource.getId());
        dto.setQuantity(2);
        dto.setDueDate(LocalDate.now().plusDays(5));

        CheckoutResponseDto response = circulationTransactionalService.executeCheckout(dto, admin.getId());
        assertNotNull(response);
        assertEquals(2, response.getQuantityIssued());
        assertEquals(1, response.getReservationRemainderReleased(), "C30/C38: Exactly 1 remainder unit must be released");

        // Verify remaining 1 unit emitted STOCK_REPLENISHED event
        List<OutboxEvent> events = outboxEventRepository.findAll();
        boolean foundReplenished = events.stream()
                .filter(e -> "STOCK_REPLENISHED".equals(e.getEventType()))
                .anyMatch(e -> e.getPayload().contains("PARTIAL_CHECKOUT_REMAINDER") && e.getPayload().contains("releasedQuantity"));
        assertTrue(foundReplenished, "Must publish STOCK_REPLENISHED outbox event for remainder quantity");
    }

    @Test
    @DisplayName("C42: Zero JVM business clock - issueDate is populated by MySQL CURRENT_DATE()")
    void testC42DatabaseClockForBusinessDates() {
        User student = userRepository.findByUsername("student1").orElseThrow();
        User admin = userRepository.findByUsername("admin").orElseThrow();
        Resource bulkResource = resourceRepository.findAll().stream()
                .filter(r -> r.getInventoryMode() == InventoryMode.QUANTITY)
                .findFirst()
                .orElseThrow();

        Reservation res = createTestReservation(student, bulkResource, 1, Instant.now().plusSeconds(3600));

        CheckoutRequestDto dto = new CheckoutRequestDto();
        dto.setReservationId(res.getId());
        dto.setUserId(student.getId());
        dto.setResourceId(bulkResource.getId());
        dto.setQuantity(1);
        dto.setDueDate(LocalDate.now().plusDays(7));

        CheckoutResponseDto response = circulationTransactionalService.executeCheckout(dto, admin.getId());
        assertNotNull(response.getIssueDate(), "Issue date must be generated by database");

        // Verify database value directly
        java.sql.Date dbDate = jdbcTemplate.queryForObject(
                "SELECT issue_date FROM borrowing_records WHERE id = ?",
                java.sql.Date.class,
                response.getBorrowingRecordId()
        );
        assertNotNull(dbDate);
        assertEquals(response.getIssueDate(), dbDate.toLocalDate());
    }

    @Test
    @DisplayName("C47/C48: JIT single-event claim with SKIP LOCKED and conditional fencing guard")
    void testC47AndC48OutboxJitClaimAndConditionalFencing() {
        // Create an outbox event
        OutboxEvent event = new OutboxEvent();
        event.setEventType("TEST_EVENT");
        event.setAggregateId(999L);
        event.setPayload("{\"test\":true}");
        event.setStatus(OutboxStatus.PENDING);
        event.setFenceGeneration(0L);
        event = outboxEventRepository.save(event);

        final Long eventId = event.getId();
        String workerId = "test-worker-" + UUID.randomUUID();

        TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        // Claim event
        tt.executeWithoutResult(status -> {
            int claimed = outboxEventRepository.claimEvent(eventId, workerId, 30);
            assertEquals(1, claimed, "Must successfully claim event");
        });

        OutboxEvent claimedEvent = outboxEventRepository.findById(eventId).orElseThrow();
        assertEquals(OutboxStatus.IN_FLIGHT, claimedEvent.getStatus());
        assertEquals(workerId, claimedEvent.getClaimedBy());
        assertEquals(1L, claimedEvent.getFenceGeneration());

        // Attempt completion with wrong fence generation (fenced zombie test)
        tt.executeWithoutResult(status -> {
            int wrongFenceUpdated = outboxEventRepository.markCompletedWithFencing(eventId, workerId, 999L);
            assertEquals(0, wrongFenceUpdated, "Completion guard must reject update when fence generation mismatches");
        });

        // Attempt completion with wrong worker ID
        tt.executeWithoutResult(status -> {
            int wrongWorkerUpdated = outboxEventRepository.markCompletedWithFencing(eventId, "zombie-worker", 1L);
            assertEquals(0, wrongWorkerUpdated, "Completion guard must reject update when worker ID mismatches");
        });

        // Successful completion with matching worker ID and fence generation
        tt.executeWithoutResult(status -> {
            int validUpdated = outboxEventRepository.markCompletedWithFencing(eventId, workerId, 1L);
            assertEquals(1, validUpdated, "Valid completion must update exactly 1 row");
        });

        OutboxEvent completedEvent = outboxEventRepository.findById(eventId).orElseThrow();
        assertEquals(OutboxStatus.PROCESSED, completedEvent.getStatus());
        assertNull(completedEvent.getClaimedBy());
    }

    @Test
    @DisplayName("BUG-08: CirculationRetryOrchestrator correctly serializes LocalDate in CheckoutRequestDto with Spring ObjectMapper")
    void testBug08LocalDateSerialization() {
        CheckoutRequestDto dto = new CheckoutRequestDto();
        dto.setResourceId(6L);
        dto.setUserId(1L);
        dto.setQuantity(1);
        dto.setDueDate(LocalDate.now().plusDays(7));
        dto.setNotes("BUG-08 regression test verification");

        // Verify that Spring-injected ObjectMapper serializes and deserializes JSR-310 LocalDate
        assertDoesNotThrow(() -> {
            String json = objectMapper.writeValueAsString(dto);
            assertTrue(json.contains(dto.getDueDate().toString()), "Serialized JSON must contain ISO-8601 formatted LocalDate");
            CheckoutRequestDto deserialized = objectMapper.readValue(json, CheckoutRequestDto.class);
            assertEquals(dto.getDueDate(), deserialized.getDueDate(), "Deserialized LocalDate must match original");
        }, "Serialization of CheckoutRequestDto with LocalDate must not throw InvalidDefinitionException");
    }
}
