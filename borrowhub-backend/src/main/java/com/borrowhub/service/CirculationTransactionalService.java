package com.borrowhub.service;

import com.borrowhub.exception.BadRequestException;
import com.borrowhub.exception.ConflictException;
import com.borrowhub.exception.ReservationExpiredException;
import com.borrowhub.exception.ResourceNotFoundException;
import com.borrowhub.model.entity.*;
import com.borrowhub.model.enums.*;
import com.borrowhub.payload.request.CheckoutRequestDto;
import com.borrowhub.payload.request.ReturnRequestDto;
import com.borrowhub.payload.response.CheckoutResponseDto;
import com.borrowhub.payload.response.ReturnResponseDto;
import com.borrowhub.repository.*;
import com.borrowhub.repository.projection.ReservationMetaProjection;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Core Circulation Transactional Service.
 * Implements strict DAG Lock Ordering (C43, C36):
 * Level 1: Resource
 * Level 2: ResourceUnits (ASC order)
 * Level 3: Reservation
 * Level 4: BorrowingRecord
 * Level 6: OutboxEvent
 *
 * Implements C42 zero JVM clock for issueDate, C17 stored generated column sync,
 * C30/C38 partial quantity checkout remainder release, and C12/C13 return semantics.
 */
@Service
public class CirculationTransactionalService {

    private static final Logger logger = LoggerFactory.getLogger(CirculationTransactionalService.class);

    private final ResourceRepository resourceRepository;
    private final ResourceUnitRepository resourceUnitRepository;
    private final BorrowingRequestRepository borrowingRequestRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationUnitRepository reservationUnitRepository;
    private final BorrowingRecordRepository borrowingRecordRepository;
    private final UserRepository userRepository;
    private final ReservationService reservationService;
    private final OutboxService outboxService;
    private final EntityManager entityManager;

    public CirculationTransactionalService(ResourceRepository resourceRepository,
                                           ResourceUnitRepository resourceUnitRepository,
                                           BorrowingRequestRepository borrowingRequestRepository,
                                           ReservationRepository reservationRepository,
                                           ReservationUnitRepository reservationUnitRepository,
                                           BorrowingRecordRepository borrowingRecordRepository,
                                           UserRepository userRepository,
                                           ReservationService reservationService,
                                           OutboxService outboxService,
                                           EntityManager entityManager) {
        this.resourceRepository = resourceRepository;
        this.resourceUnitRepository = resourceUnitRepository;
        this.borrowingRequestRepository = borrowingRequestRepository;
        this.reservationRepository = reservationRepository;
        this.reservationUnitRepository = reservationUnitRepository;
        this.borrowingRecordRepository = borrowingRecordRepository;
        this.userRepository = userRepository;
        this.reservationService = reservationService;
        this.outboxService = outboxService;
        this.entityManager = entityManager;
    }

    /**
     * Executes Checkout with strict DAG Level 1 -> 2 -> 3 -> 4 -> 6 lock ordering.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CheckoutResponseDto executeCheckout(CheckoutRequestDto dto, Long staffUserId) {
        User staffUser = userRepository.findById(staffUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff User", "id", staffUserId));

        User borrower = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Borrower", "id", dto.getUserId()));

        Long reservationId = dto.getReservationId();
        Long resourceId = dto.getResourceId();
        List<Long> requestedUnitIds = dto.getUnitIds() != null ? new ArrayList<>(dto.getUnitIds()) : new ArrayList<>();

        // Step 1: Non-locking metadata discovery if reservation is provided (C43 Step 1)
        if (reservationId != null) {
            ReservationMetaProjection meta = reservationRepository.findMetaById(reservationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Reservation", "id", reservationId));
            resourceId = meta.getResourceId();

            if (requestedUnitIds.isEmpty()) {
                List<Long> mappedIds = reservationUnitRepository.findMappedUnitIds(reservationId);
                requestedUnitIds.addAll(mappedIds);
            }
        }

        // Step 2: Global lock acquisition strictly following C43 DAG hierarchy
        // Level 1: Resource lock
        final Long targetResourceId = resourceId;
        Resource resource = resourceRepository.findByIdForUpdate(targetResourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource", "id", targetResourceId));

        if (!resource.isActive()) {
            throw new BadRequestException("Resource is inactive");
        }

        // Level 2: ResourceUnit lock (strictly sorted ascending by Primary Key)
        List<ResourceUnit> lockedUnits = new ArrayList<>();
        if (!requestedUnitIds.isEmpty()) {
            Collections.sort(requestedUnitIds);
            lockedUnits = resourceUnitRepository.findUnitsByIdsForUpdate(requestedUnitIds);
            if (lockedUnits.size() != requestedUnitIds.size()) {
                throw new ConflictException("One or more requested asset units could not be locked");
            }
        }

        // Level 3: Reservation lock
        Reservation reservation = null;
        int reservationRemainderReleased = 0;

        if (reservationId != null) {
            reservation = reservationRepository.findByIdForUpdate(reservationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Reservation", "id", reservationId));

            // Validate status
            if (reservation.getStatus() != ReservationStatus.HELD) {
                throw new BadRequestException("Reservation is not in HELD status. Current status: " + reservation.getStatus());
            }

            // C21: Check deadline against authoritative time
            if (reservation.getClaimDeadline().isBefore(Instant.now())) {
                reservationService.expireReservationDurable(reservation.getId());
                throw new ReservationExpiredException("Reservation hold expired at " + reservation.getClaimDeadline());
            }

            // C30/C38: Partial quantity checkout remainder management
            int reservedQty = reservation.getQuantity();
            int checkoutQty = dto.getQuantity();
            if (checkoutQty > reservedQty) {
                throw new BadRequestException("Checkout quantity (" + checkoutQty + ") cannot exceed reserved quantity (" + reservedQty + ")");
            }

            if (checkoutQty < reservedQty) {
                reservationRemainderReleased = reservedQty - checkoutQty;
                logger.info("Partial checkout detected: checked out {}, reserved {}. Releasing {} remainder.",
                        checkoutQty, reservedQty, reservationRemainderReleased);

                // Publish STOCK_REPLENISHED outbox event for remainder
                outboxService.publishEvent(
                        "STOCK_REPLENISHED",
                        "RESOURCE",
                        resource.getId(),
                        String.format("{\"resourceId\":%d,\"releasedQuantity\":%d,\"reason\":\"PARTIAL_CHECKOUT_REMAINDER\"}",
                                resource.getId(), reservationRemainderReleased)
                );
            }

            // Mark reservation CLAIMED
            reservation.setStatus(ReservationStatus.CLAIMED);
            reservationRepository.save(reservation);
        } else {
            // Walk-in / Direct checkout at desk without prior reservation: create immediate auto-approved request and auto-claimed reservation
            BorrowingRequest autoReq = new BorrowingRequest();
            autoReq.setRequestNumber("REQ-WALKIN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            autoReq.setUser(borrower);
            autoReq.setResource(resource);
            autoReq.setQuantity(dto.getQuantity());
            autoReq.setRequestedStart(LocalDate.now());
            autoReq.setRequestedEnd(dto.getDueDate());
            autoReq.setPurpose("Direct Desk Checkout");
            autoReq.setStatus(RequestStatus.APPROVED);
            autoReq.setReviewedBy(staffUser);
            autoReq.setReviewedAt(Instant.now());
            autoReq = borrowingRequestRepository.save(autoReq);

            reservation = new Reservation();
            reservation.setRequest(autoReq);
            reservation.setReservationNumber("RES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            reservation.setUser(borrower);
            reservation.setResource(resource);
            reservation.setQuantity(dto.getQuantity());
            reservation.setStartDate(LocalDate.now());
            reservation.setEndDate(dto.getDueDate());
            reservation.setStatus(ReservationStatus.CLAIMED);
            reservation.setClaimDeadline(Instant.now().plusSeconds(60));
            reservation = reservationRepository.save(reservation);
        }

        // Level 4: Create BorrowingRecord
        BorrowingRecord record = new BorrowingRecord();
        record.setTransactionCode("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        record.setUser(borrower);
        record.setResource(resource);
        record.setReservation(reservation);
        record.setQuantityIssued(dto.getQuantity());
        record.setQuantityReturned(0);
        record.setQuantityDamaged(0);
        record.setQuantityLost(0);
        record.setDueDate(dto.getDueDate());
        record.setConditionAtIssue(PhysicalCondition.GOOD);
        record.setIssuedBy(staffUser);
        record.setStatus(BorrowingStatus.ACTIVE);
        record.setRemarks(dto.getNotes());

        List<String> issuedTags = new ArrayList<>();
        if (resource.getInventoryMode() == InventoryMode.INDIVIDUAL) {
            if (lockedUnits.isEmpty()) {
                throw new BadRequestException("Individual asset requires unit identification for checkout");
            }
            record.setResourceUnit(lockedUnits.get(0));
            for (ResourceUnit u : lockedUnits) {
                u.setStatus(UnitStatus.BORROWED);
                resourceUnitRepository.save(u);
                issuedTags.add(u.getAssetCode());
            }
        }

        // Note: C42 zero JVM clock: issueDate is NOT set here.
        // It is generated by MySQL DEFAULT (CURRENT_DATE()).
        BorrowingRecord savedRecord = borrowingRecordRepository.save(record);
        entityManager.flush();
        entityManager.refresh(savedRecord); // Reloads database-generated issue_date & stored quantity_outstanding (C17)

        CheckoutResponseDto response = new CheckoutResponseDto();
        response.setBorrowingRecordId(savedRecord.getId());
        response.setResourceId(resource.getId());
        response.setResourceName(resource.getName());
        response.setUserId(borrower.getId());
        response.setUserName(borrower.getFullName());
        response.setQuantityIssued(savedRecord.getQuantityIssued());
        response.setQuantityOutstanding(savedRecord.getQuantityOutstanding());
        response.setIssueDate(savedRecord.getIssueDate());
        response.setDueDate(savedRecord.getDueDate());
        response.setStatus(savedRecord.getStatus());
        response.setIssuedAssetTags(issuedTags);
        response.setReservationId(reservation.getId());
        response.setReservationRemainderReleased(reservationRemainderReleased);

        return response;
    }

    /**
     * Executes Return with strict DAG Level 1 -> 2 -> 4 -> 6 lock ordering.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ReturnResponseDto executeReturn(ReturnRequestDto dto, Long staffUserId) {
        User staffUser = userRepository.findById(staffUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff User", "id", staffUserId));

        // Discover borrowing record non-locking to find resource and unit
        BorrowingRecord recordMeta = borrowingRecordRepository.findById(dto.getBorrowingRecordId())
                .orElseThrow(() -> new ResourceNotFoundException("BorrowingRecord", "id", dto.getBorrowingRecordId()));

        Long resourceId = recordMeta.getResource().getId();
        Long unitId = recordMeta.getResourceUnit() != null ? recordMeta.getResourceUnit().getId() : null;

        // Level 1: Resource lock
        Resource resource = resourceRepository.findByIdForUpdate(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource", "id", resourceId));

        // Level 2: ResourceUnit lock
        ResourceUnit unit = null;
        if (unitId != null) {
            unit = resourceUnitRepository.findByIdForUpdate(unitId)
                    .orElseThrow(() -> new ResourceNotFoundException("ResourceUnit", "id", unitId));
        }

        // Level 4: BorrowingRecord lock
        BorrowingRecord record = borrowingRecordRepository.findByIdForUpdate(dto.getBorrowingRecordId())
                .orElseThrow(() -> new ResourceNotFoundException("BorrowingRecord", "id", dto.getBorrowingRecordId()));

        // Validate status: must be active custody
        List<BorrowingStatus> activeStatuses = List.of(
                BorrowingStatus.ACTIVE,
                BorrowingStatus.PARTIALLY_RETURNED,
                BorrowingStatus.OVERDUE,
                BorrowingStatus.RETURN_PENDING
        );
        if (!activeStatuses.contains(record.getStatus())) {
            throw new BadRequestException("Borrowing record is not in active custody status. Current status: " + record.getStatus());
        }

        // C17 Stored Column Sync: compute outstanding
        int currentOutstanding = record.getQuantityIssued() - record.getQuantityReturned() - record.getQuantityLost();
        int returnQty = dto.getQuantityReturned();

        if (returnQty <= 0) {
            throw new BadRequestException("Return quantity must be greater than zero");
        }
        if (returnQty > currentOutstanding) {
            throw new BadRequestException("Return quantity (" + returnQty + ") exceeds outstanding quantity (" + currentOutstanding + ")");
        }

        // Update return counters
        record.setQuantityReturned(record.getQuantityReturned() + returnQty);
        record.setReturnedTo(staffUser);

        PhysicalCondition condition = dto.getReturnCondition() != null ? dto.getReturnCondition() : PhysicalCondition.GOOD;
        record.setConditionAtReturn(condition);

        if (condition == PhysicalCondition.DAMAGED || condition == PhysicalCondition.POOR) {
            record.setQuantityDamaged(record.getQuantityDamaged() + returnQty);
            if (unit != null) {
                unit.setStatus(UnitStatus.MAINTENANCE);
                unit.setPhysicalCondition(condition);
                unit.setNotes(dto.getDamageNotes());
                resourceUnitRepository.save(unit);
            }
        } else {
            if (unit != null) {
                unit.setStatus(UnitStatus.AVAILABLE);
                unit.setPhysicalCondition(condition);
                resourceUnitRepository.save(unit);
            }
        }

        // C12 & C13 Terminal Status Consistency Check
        int newOutstanding = record.getQuantityIssued() - record.getQuantityReturned() - record.getQuantityLost();
        boolean fullyReturned = (newOutstanding == 0);

        if (fullyReturned) {
            if (record.getQuantityLost() == 0) {
                // Pure return
                record.setStatus(BorrowingStatus.RETURNED);
                record.setActualReturnDate(LocalDate.now());
                record.setClosedAt(null);
            } else {
                // Mixed outcome closed
                record.setStatus(BorrowingStatus.CLOSED);
                record.setClosedAt(Instant.now());
                if (record.getQuantityReturned() > 0) {
                    record.setActualReturnDate(LocalDate.now());
                }
            }
        } else {
            // Partially returned: C12 active custody window retained
            if (record.getDueDate().isBefore(LocalDate.now())) {
                record.setStatus(BorrowingStatus.OVERDUE);
            } else {
                record.setStatus(BorrowingStatus.PARTIALLY_RETURNED);
            }
        }

        BorrowingRecord savedRecord = borrowingRecordRepository.save(record);
        entityManager.flush();
        entityManager.refresh(savedRecord);

        // Level 6 Outbox Event: STOCK_REPLENISHED for returned available stock
        if (condition != PhysicalCondition.DAMAGED && condition != PhysicalCondition.POOR) {
            outboxService.publishEvent(
                    "STOCK_REPLENISHED",
                    "RESOURCE",
                    resource.getId(),
                    String.format("{\"resourceId\":%d,\"releasedQuantity\":%d,\"reason\":\"LOAN_RETURNED\"}",
                            resource.getId(), returnQty)
            );
        }

        ReturnResponseDto response = new ReturnResponseDto();
        response.setBorrowingRecordId(savedRecord.getId());
        response.setResourceId(resource.getId());
        response.setResourceName(resource.getName());
        response.setQuantityIssued(savedRecord.getQuantityIssued());
        response.setQuantityReturned(savedRecord.getQuantityReturned());
        response.setQuantityLost(savedRecord.getQuantityLost());
        response.setQuantityOutstanding(savedRecord.getQuantityOutstanding());
        response.setStatus(savedRecord.getStatus());
        response.setActualReturnDate(savedRecord.getActualReturnDate());
        response.setClosedAt(savedRecord.getClosedAt() != null ? java.time.LocalDateTime.ofInstant(savedRecord.getClosedAt(), java.time.ZoneId.of("Asia/Kolkata")) : null);
        response.setFullyReturned(fullyReturned);
        response.setMessage(fullyReturned ? "Item returned completely" : "Partial return recorded; remaining units still active in custody");

        return response;
    }
}
