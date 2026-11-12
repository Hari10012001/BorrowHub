package com.borrowhub.service;

import com.borrowhub.exception.BadRequestException;
import com.borrowhub.exception.ResourceNotFoundException;
import com.borrowhub.model.entity.Resource;
import com.borrowhub.model.entity.User;
import com.borrowhub.model.entity.WaitlistEntry;
import com.borrowhub.model.enums.Role;
import com.borrowhub.model.enums.WaitlistStatus;
import com.borrowhub.payload.request.WaitlistJoinRequest;
import com.borrowhub.payload.response.WaitlistEntryResponseDto;
import com.borrowhub.repository.ResourceRepository;
import com.borrowhub.repository.UserRepository;
import com.borrowhub.repository.WaitlistEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WaitlistService {

    private static final Logger logger = LoggerFactory.getLogger(WaitlistService.class);

    private final WaitlistEntryRepository waitlistEntryRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final OutboxService outboxService;

    public WaitlistService(WaitlistEntryRepository waitlistEntryRepository,
                           ResourceRepository resourceRepository,
                           UserRepository userRepository,
                           OutboxService outboxService) {
        this.waitlistEntryRepository = waitlistEntryRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
        this.outboxService = outboxService;
    }

    @Transactional
    public WaitlistEntryResponseDto joinWaitlist(Long userId, WaitlistJoinRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Resource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Resource", "id", request.getResourceId()));

        if (!resource.isActive()) {
            throw new BadRequestException("Resource is inactive");
        }

        // Priority Tier: 1 for FACULTY and STAFF, 2 for STUDENT
        int priorityTier = (user.getRole() == Role.FACULTY || user.getRole() == Role.ADMIN) ? 1 : 2;
        int nextPosition = waitlistEntryRepository.findMaxQueuePosition(resource.getId()) + 1;

        WaitlistEntry entry = new WaitlistEntry();
        entry.setUser(user);
        entry.setResource(resource);
        entry.setPriorityTier(priorityTier);
        entry.setQueuePosition(nextPosition);
        entry.setRequestedStart(request.getNeededFrom());
        entry.setRequestedEnd(request.getNeededUntil());
        entry.setStatus(WaitlistStatus.WAITING);

        WaitlistEntry saved = waitlistEntryRepository.save(entry);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<WaitlistEntryResponseDto> getMyWaitlist(Long userId) {
        List<WaitlistEntry> list = waitlistEntryRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return list.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    public void cancelWaitlist(Long entryId, Long userId) {
        WaitlistEntry entry = waitlistEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("WaitlistEntry", "id", entryId));

        if (!entry.getUser().getId().equals(userId)) {
            throw new BadRequestException("You can only cancel your own waitlist entries");
        }

        entry.setStatus(WaitlistStatus.CANCELLED);
        waitlistEntryRepository.save(entry);
    }

    /**
     * Promotes the next waiting waitlist entry when stock is replenished.
     * Level 5 Lock acquisition inside transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean promoteNextInQueue(Long resourceId) {
        List<WaitlistEntry> candidates = waitlistEntryRepository
                .findByResourceIdAndStatusOrderByPriorityTierAscQueuePositionAsc(resourceId, WaitlistStatus.WAITING);

        if (candidates.isEmpty()) {
            return false;
        }

        WaitlistEntry candidate = candidates.get(0);
        WaitlistEntry lockedEntry = waitlistEntryRepository.findByIdForUpdate(candidate.getId())
                .orElse(null);

        if (lockedEntry != null && lockedEntry.getStatus() == WaitlistStatus.WAITING) {
            lockedEntry.setStatus(WaitlistStatus.NOTIFIED);
            lockedEntry.setNotifiedAt(Instant.now());
            lockedEntry.setClaimDeadline(Instant.now().plus(24, ChronoUnit.HOURS)); // 24-hr claim window
            waitlistEntryRepository.save(lockedEntry);

            // Level 6 Outbox Event: WAITLIST_PROMOTED
            outboxService.publishEvent(
                    "WAITLIST_PROMOTED",
                    "WAITLIST_ENTRY",
                    lockedEntry.getId(),
                    String.format("{\"waitlistEntryId\":%d,\"userId\":%d,\"resourceId\":%d,\"claimDeadline\":\"%s\"}",
                            lockedEntry.getId(), lockedEntry.getUser().getId(), resourceId, lockedEntry.getClaimDeadline())
            );

            logger.info("Promoted waitlist entry {} for user {} on resource {}",
                    lockedEntry.getId(), lockedEntry.getUser().getId(), resourceId);
            return true;
        }

        return false;
    }

    private WaitlistEntryResponseDto mapToResponse(WaitlistEntry entry) {
        WaitlistEntryResponseDto dto = new WaitlistEntryResponseDto();
        dto.setId(entry.getId());
        dto.setResourceId(entry.getResource().getId());
        dto.setResourceName(entry.getResource().getName());
        dto.setUserId(entry.getUser().getId());
        dto.setUserName(entry.getUser().getFullName());
        dto.setQuantity(1);
        dto.setPriorityTier(entry.getPriorityTier());
        dto.setNeededFrom(entry.getRequestedStart());
        dto.setNeededUntil(entry.getRequestedEnd());
        dto.setStatus(entry.getStatus());
        dto.setPositionInQueue(entry.getQueuePosition());
        if (entry.getNotifiedAt() != null) {
            dto.setNotifiedAt(LocalDateTime.ofInstant(entry.getNotifiedAt(), ZoneId.of("Asia/Kolkata")));
        }
        if (entry.getClaimDeadline() != null) {
            dto.setClaimDeadline(LocalDateTime.ofInstant(entry.getClaimDeadline(), ZoneId.of("Asia/Kolkata")));
        }
        dto.setCreatedAt(LocalDateTime.ofInstant(entry.getCreatedAt(), ZoneId.of("Asia/Kolkata")));
        return dto;
    }
}
