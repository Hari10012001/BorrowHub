package com.borrowhub.service;

import com.borrowhub.exception.BadRequestException;
import com.borrowhub.exception.ConflictException;
import com.borrowhub.exception.ReservationExpiredException;
import com.borrowhub.exception.ResourceNotFoundException;
import com.borrowhub.model.entity.*;
import com.borrowhub.model.enums.*;
import com.borrowhub.payload.request.ReservationCreateDto;
import com.borrowhub.payload.response.ReservationResponseDto;
import com.borrowhub.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    private static final Logger logger = LoggerFactory.getLogger(ReservationService.class);

    private final ReservationRepository reservationRepository;
    private final ReservationUnitRepository reservationUnitRepository;
    private final ResourceRepository resourceRepository;
    private final ResourceUnitRepository resourceUnitRepository;
    private final UserRepository userRepository;
    private final BorrowingRequestRepository borrowingRequestRepository;
    private final AvailabilityService availabilityService;
    private final OutboxService outboxService;

    public ReservationService(ReservationRepository reservationRepository,
                              ReservationUnitRepository reservationUnitRepository,
                              ResourceRepository resourceRepository,
                              ResourceUnitRepository resourceUnitRepository,
                              UserRepository userRepository,
                              BorrowingRequestRepository borrowingRequestRepository,
                              AvailabilityService availabilityService,
                              OutboxService outboxService) {
        this.reservationRepository = reservationRepository;
        this.reservationUnitRepository = reservationUnitRepository;
        this.resourceRepository = resourceRepository;
        this.resourceUnitRepository = resourceUnitRepository;
        this.userRepository = userRepository;
        this.borrowingRequestRepository = borrowingRequestRepository;
        this.availabilityService = availabilityService;
        this.outboxService = outboxService;
    }

    @Transactional
    public ReservationResponseDto createReservation(Long userId, ReservationCreateDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Resource resource = resourceRepository.findById(dto.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Resource", "id", dto.getResourceId()));

        if (!resource.isActive()) {
            throw new BadRequestException("Resource is inactive");
        }
        if (!resource.isReservable()) {
            throw new BadRequestException("Resource is not reservable");
        }

        int available = availabilityService.getAvailableQuantity(resource.getId());
        if (available < dto.getQuantity()) {
            throw new ConflictException("INSUFFICIENT_STOCK",
                    "Insufficient available inventory. Current available: " + available + ", Requested: " + dto.getQuantity());
        }

        // Create auto-fulfilled request for relational tracking
        BorrowingRequest request = new BorrowingRequest();
        request.setRequestNumber("REQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        request.setUser(user);
        request.setResource(resource);
        request.setQuantity(dto.getQuantity());
        request.setRequestedStart(dto.getStartDate());
        request.setRequestedEnd(dto.getEndDate());
        request.setPurpose("Direct Online Reservation");
        request.setStatus(RequestStatus.APPROVED);
        request.setReviewedBy(user);
        request.setReviewedAt(Instant.now());
        borrowingRequestRepository.save(request);

        // Create Reservation
        Reservation reservation = new Reservation();
        reservation.setReservationNumber("RES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        reservation.setRequest(request);
        reservation.setUser(user);
        reservation.setResource(resource);
        reservation.setQuantity(dto.getQuantity());
        reservation.setStartDate(dto.getStartDate());
        reservation.setEndDate(dto.getEndDate());
        reservation.setStatus(ReservationStatus.HELD);
        // Hold claim deadline: 48 hours from now
        reservation.setClaimDeadline(Instant.now().plus(48, ChronoUnit.HOURS));

        Reservation savedReservation = reservationRepository.save(reservation);

        // Handle INDIVIDUAL mode units
        List<String> assetTags = new ArrayList<>();
        if (resource.getInventoryMode() == InventoryMode.INDIVIDUAL) {
            List<ResourceUnit> unitsToAllocate;
            if (dto.getRequestedUnitIds() != null && !dto.getRequestedUnitIds().isEmpty()) {
                unitsToAllocate = resourceUnitRepository.findAllById(dto.getRequestedUnitIds());
            } else {
                List<ResourceUnit> availableUnits = availabilityService.getAvailableUnits(resource.getId());
                if (availableUnits.size() < dto.getQuantity()) {
                    throw new ConflictException("INSUFFICIENT_UNITS", "Requested individual units are no longer available");
                }
                unitsToAllocate = availableUnits.subList(0, dto.getQuantity());
            }

            for (ResourceUnit unit : unitsToAllocate) {
                ReservationUnit ru = new ReservationUnit();
                ru.setReservation(savedReservation);
                ru.setResourceUnit(unit);
                ru.setAllocatedFrom(dto.getStartDate());
                ru.setAllocatedUntil(dto.getEndDate());
                reservationUnitRepository.save(ru);
                assetTags.add(unit.getAssetCode());
            }
        }

        return mapToResponse(savedReservation, assetTags);
    }

    @Transactional
    public void cancelReservation(Long reservationId, Long userId, String reason) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", "id", reservationId));

        if (!reservation.getUser().getId().equals(userId)) {
            User user = userRepository.findById(userId).orElse(null);
            boolean isStaffOrAdmin = user != null && (user.getRole() == Role.ADMIN || user.getRole() == Role.FACULTY);
            if (!isStaffOrAdmin) {
                throw new BadRequestException("You can only cancel your own reservations");
            }
        }

        if (reservation.getStatus() != ReservationStatus.HELD) {
            throw new BadRequestException("Only HELD reservations can be cancelled");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancellationReason(reason);
        reservationRepository.save(reservation);

        // Delete allocated reservation units
        reservationUnitRepository.deleteAllByReservationId(reservationId);

        // Emit STOCK_REPLENISHED event to notify waitlist
        outboxService.publishEvent(
                "STOCK_REPLENISHED",
                "RESOURCE",
                reservation.getResource().getId(),
                String.format("{\"resourceId\":%d,\"releasedQuantity\":%d,\"reason\":\"RESERVATION_CANCELLED\"}",
                        reservation.getResource().getId(), reservation.getQuantity())
        );
    }

    /**
     * C21: Expiry transition is committed in a dedicated transaction before raising
     * any exception, ensuring durable persistence of EXPIRED status.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void expireReservationDurable(Long reservationId) {
        Reservation reservation = reservationRepository.findByIdForUpdate(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", "id", reservationId));

        if (reservation.getStatus() == ReservationStatus.HELD) {
            reservation.setStatus(ReservationStatus.EXPIRED);
            reservation.setCancellationReason("Claim deadline elapsed without checkout");
            reservationRepository.save(reservation);

            // Free allocated units
            reservationUnitRepository.deleteAllByReservationId(reservationId);

            // Level 6 outbox event
            outboxService.publishEvent(
                    "RESERVATION_EXPIRED",
                    "RESERVATION",
                    reservation.getId(),
                    String.format("{\"reservationId\":%d,\"resourceId\":%d,\"quantity\":%d}",
                            reservation.getId(), reservation.getResource().getId(), reservation.getQuantity())
            );

            outboxService.publishEvent(
                    "STOCK_REPLENISHED",
                    "RESOURCE",
                    reservation.getResource().getId(),
                    String.format("{\"resourceId\":%d,\"releasedQuantity\":%d,\"reason\":\"RESERVATION_EXPIRED\"}",
                            reservation.getResource().getId(), reservation.getQuantity())
            );

            logger.info("Reservation {} durably marked EXPIRED", reservationId);
        }
    }

    @Transactional(readOnly = true)
    public List<ReservationResponseDto> getMyReservations(Long userId) {
        List<Reservation> list = reservationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return list.stream().map(r -> {
            List<String> tags = reservationUnitRepository.findAssetCodesByReservationId(r.getId());
            return mapToResponse(r, tags);
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReservationResponseDto getReservationById(Long id) {
        Reservation r = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", "id", id));
        List<String> tags = reservationUnitRepository.findAssetCodesByReservationId(r.getId());
        return mapToResponse(r, tags);
    }

    private ReservationResponseDto mapToResponse(Reservation r, List<String> assetTags) {
        ReservationResponseDto dto = new ReservationResponseDto();
        dto.setId(r.getId());
        dto.setResourceId(r.getResource().getId());
        dto.setResourceName(r.getResource().getName());
        dto.setUserId(r.getUser().getId());
        dto.setUserName(r.getUser().getFullName());
        dto.setQuantityReserved(r.getQuantity());
        dto.setStartDate(r.getStartDate());
        dto.setEndDate(r.getEndDate());
        dto.setStatus(r.getStatus());
        dto.setHoldExpiresAt(LocalDateTime.ofInstant(r.getClaimDeadline(), ZoneId.of("Asia/Kolkata")));
        dto.setReservedAssetTags(assetTags);
        dto.setCreatedAt(LocalDateTime.ofInstant(r.getCreatedAt(), ZoneId.of("Asia/Kolkata")));
        return dto;
    }
}
