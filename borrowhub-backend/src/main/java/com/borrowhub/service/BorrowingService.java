package com.borrowhub.service;

import com.borrowhub.exception.ResourceNotFoundException;
import com.borrowhub.model.entity.BorrowingRecord;
import com.borrowhub.payload.response.BorrowingRecordResponseDto;
import com.borrowhub.repository.BorrowingRecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BorrowingService {

    private final BorrowingRecordRepository borrowingRecordRepository;

    public BorrowingService(BorrowingRecordRepository borrowingRecordRepository) {
        this.borrowingRecordRepository = borrowingRecordRepository;
    }

    @Transactional(readOnly = true)
    public List<BorrowingRecordResponseDto> getMyBorrowings(Long userId) {
        List<BorrowingRecord> list = borrowingRecordRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return list.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BorrowingRecordResponseDto getRecordById(Long id) {
        BorrowingRecord record = borrowingRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BorrowingRecord", "id", id));
        return mapToResponse(record);
    }

    @Transactional(readOnly = true)
    public Page<BorrowingRecordResponseDto> getAllRecords(Pageable pageable) {
        Page<BorrowingRecord> page = borrowingRecordRepository.findAllByOrderByCreatedAtDesc(pageable);
        return page.map(this::mapToResponse);
    }

    private BorrowingRecordResponseDto mapToResponse(BorrowingRecord r) {
        BorrowingRecordResponseDto dto = new BorrowingRecordResponseDto();
        dto.setId(r.getId());
        dto.setUserId(r.getUser().getId());
        dto.setUserName(r.getUser().getFullName());
        dto.setResourceId(r.getResource().getId());
        dto.setResourceName(r.getResource().getName());
        dto.setQuantityIssued(r.getQuantityIssued());
        dto.setQuantityReturned(r.getQuantityReturned());
        dto.setQuantityLost(r.getQuantityLost());
        dto.setQuantityOutstanding(r.getQuantityOutstanding());
        dto.setIssueDate(r.getIssueDate());
        dto.setDueDate(r.getDueDate());
        dto.setActualReturnDate(r.getActualReturnDate());
        dto.setStatus(r.getStatus());
        if (r.getReservation() != null) {
            dto.setReservationId(r.getReservation().getId());
        }
        if (r.getClosedAt() != null) {
            dto.setClosedAt(LocalDateTime.ofInstant(r.getClosedAt(), ZoneId.of("Asia/Kolkata")));
        }
        List<String> tags = new ArrayList<>();
        if (r.getResourceUnit() != null) {
            tags.add(r.getResourceUnit().getAssetCode());
        }
        dto.setAssetTags(tags);
        dto.setCreatedAt(LocalDateTime.ofInstant(r.getCreatedAt(), ZoneId.of("Asia/Kolkata")));
        return dto;
    }
}
