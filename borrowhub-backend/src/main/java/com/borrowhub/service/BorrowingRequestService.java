package com.borrowhub.service;

import com.borrowhub.exception.BadRequestException;
import com.borrowhub.exception.ResourceNotFoundException;
import com.borrowhub.model.entity.*;
import com.borrowhub.model.enums.*;
import com.borrowhub.payload.request.BorrowingApprovalDto;
import com.borrowhub.payload.request.BorrowingRequestCreateDto;
import com.borrowhub.payload.response.BorrowingRequestResponseDto;
import com.borrowhub.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BorrowingRequestService {

    private final BorrowingRequestRepository borrowingRequestRepository;
    private final ResourceRepository resourceRepository;
    private final ResourceUnitRepository resourceUnitRepository;
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationUnitRepository reservationUnitRepository;
    private final AvailabilityService availabilityService;

    public BorrowingRequestService(BorrowingRequestRepository borrowingRequestRepository,
                                   ResourceRepository resourceRepository,
                                   ResourceUnitRepository resourceUnitRepository,
                                   UserRepository userRepository,
                                   ReservationRepository reservationRepository,
                                   ReservationUnitRepository reservationUnitRepository,
                                   AvailabilityService availabilityService) {
        this.borrowingRequestRepository = borrowingRequestRepository;
        this.resourceRepository = resourceRepository;
        this.resourceUnitRepository = resourceUnitRepository;
        this.userRepository = userRepository;
        this.reservationRepository = reservationRepository;
        this.reservationUnitRepository = reservationUnitRepository;
        this.availabilityService = availabilityService;
    }

    @Transactional
    public BorrowingRequestResponseDto submitRequest(Long userId, BorrowingRequestCreateDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Resource resource = resourceRepository.findById(dto.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Resource", "id", dto.getResourceId()));

        if (!resource.isActive()) {
            throw new BadRequestException("Requested resource is inactive");
        }

        int available = availabilityService.getAvailableQuantity(resource.getId());
        if (available < dto.getQuantity()) {
            throw new BadRequestException("Insufficient available inventory. Available: " + available + ", Requested: " + dto.getQuantity());
        }

        BorrowingRequest request = new BorrowingRequest();
        request.setRequestNumber("REQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        request.setUser(user);
        request.setResource(resource);
        request.setQuantity(dto.getQuantity());
        request.setRequestedStart(dto.getRequestedStartDate());
        request.setRequestedEnd(dto.getRequestedEndDate());
        request.setPurpose(dto.getPurpose() != null ? dto.getPurpose() : "Academic Resource Borrowing");
        request.setStatus(RequestStatus.PENDING_REVIEW);

        BorrowingRequest saved = borrowingRequestRepository.save(request);
        return mapToResponse(saved);
    }

    @Transactional
    public BorrowingRequestResponseDto reviewRequest(Long requestId, Long reviewerId, BorrowingApprovalDto dto) {
        BorrowingRequest request = borrowingRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("BorrowingRequest", "id", requestId));

        if (request.getStatus() != RequestStatus.PENDING_REVIEW) {
            throw new BadRequestException("Request is not in PENDING_REVIEW state");
        }

        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("Reviewer", "id", reviewerId));

        request.setReviewedBy(reviewer);
        request.setReviewedAt(Instant.now());

        if (Boolean.TRUE.equals(dto.getApproved())) {
            request.setStatus(RequestStatus.APPROVED);

            // Create HELD reservation
            Reservation reservation = new Reservation();
            reservation.setReservationNumber("RES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            reservation.setRequest(request);
            reservation.setUser(request.getUser());
            reservation.setResource(request.getResource());
            reservation.setQuantity(request.getQuantity());
            reservation.setStartDate(request.getRequestedStart());
            reservation.setEndDate(request.getRequestedEnd());
            reservation.setStatus(ReservationStatus.HELD);
            // Claim deadline: 48 hours from approval
            reservation.setClaimDeadline(Instant.now().plus(48, ChronoUnit.HOURS));

            reservationRepository.save(reservation);

            // If individual tracking, allocate available units
            if (request.getResource().getInventoryMode() == InventoryMode.INDIVIDUAL) {
                List<ResourceUnit> availableUnits = availabilityService.getAvailableUnits(request.getResource().getId());
                if (availableUnits.size() < request.getQuantity()) {
                    throw new BadRequestException("Not enough individual units available for reservation");
                }
                for (int i = 0; i < request.getQuantity(); i++) {
                    ResourceUnit unit = availableUnits.get(i);
                    ReservationUnit ru = new ReservationUnit();
                    ru.setReservation(reservation);
                    ru.setResourceUnit(unit);
                    ru.setAllocatedFrom(reservation.getStartDate());
                    ru.setAllocatedUntil(reservation.getEndDate());
                    reservationUnitRepository.save(ru);
                }
            }
        } else {
            request.setStatus(RequestStatus.REJECTED);
            request.setRejectionReason(dto.getReviewNotes());
        }

        BorrowingRequest saved = borrowingRequestRepository.save(request);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<BorrowingRequestResponseDto> getMyRequests(Long userId) {
        List<BorrowingRequest> list = borrowingRequestRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return list.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BorrowingRequestResponseDto> getPendingRequests() {
        List<BorrowingRequest> list = borrowingRequestRepository.findByStatusOrderByCreatedAtAsc(RequestStatus.PENDING_REVIEW);
        return list.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private BorrowingRequestResponseDto mapToResponse(BorrowingRequest req) {
        BorrowingRequestResponseDto dto = new BorrowingRequestResponseDto();
        dto.setId(req.getId());
        dto.setUserId(req.getUser().getId());
        dto.setUserName(req.getUser().getFullName());
        dto.setResourceId(req.getResource().getId());
        dto.setResourceName(req.getResource().getName());
        dto.setQuantity(req.getQuantity());
        dto.setRequestedStartDate(req.getRequestedStart());
        dto.setRequestedEndDate(req.getRequestedEnd());
        dto.setPurpose(req.getPurpose());
        dto.setStatus(req.getStatus());
        if (req.getReviewedBy() != null) {
            dto.setReviewedByUserId(req.getReviewedBy().getId());
            dto.setReviewerName(req.getReviewedBy().getFullName());
            dto.setReviewedAt(req.getReviewedAt() != null ? java.time.LocalDateTime.ofInstant(req.getReviewedAt(), java.time.ZoneId.of("Asia/Kolkata")) : null);
            dto.setReviewNotes(req.getRejectionReason());
        }
        dto.setCreatedAt(req.getCreatedAt() != null ? java.time.LocalDateTime.ofInstant(req.getCreatedAt(), java.time.ZoneId.of("Asia/Kolkata")) : null);
        return dto;
    }
}
