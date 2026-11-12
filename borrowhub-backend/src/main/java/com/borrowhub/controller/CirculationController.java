package com.borrowhub.controller;

import com.borrowhub.payload.request.CheckoutRequestDto;
import com.borrowhub.payload.request.ReturnRequestDto;
import com.borrowhub.payload.response.ApiResponse;
import com.borrowhub.payload.response.BorrowingRecordResponseDto;
import com.borrowhub.payload.response.CheckoutResponseDto;
import com.borrowhub.payload.response.ReturnResponseDto;
import com.borrowhub.security.UserPrincipal;
import com.borrowhub.service.BorrowingService;
import com.borrowhub.service.CirculationRetryOrchestrator;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/circulation")
public class CirculationController {

    private final CirculationRetryOrchestrator circulationOrchestrator;
    private final BorrowingService borrowingService;

    public CirculationController(CirculationRetryOrchestrator circulationOrchestrator,
                                 BorrowingService borrowingService) {
        this.circulationOrchestrator = circulationOrchestrator;
        this.borrowingService = borrowingService;
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<ApiResponse<CheckoutResponseDto>> checkout(
            @Valid @RequestBody CheckoutRequestDto dto,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String xIdempotencyKey,
            @AuthenticationPrincipal UserPrincipal staffUser) {

        String effectiveKey = idempotencyKey != null ? idempotencyKey : xIdempotencyKey;
        CheckoutResponseDto response = circulationOrchestrator.checkout(dto, staffUser.getId(), effectiveKey);
        return ResponseEntity.ok(ApiResponse.success("Resource checked out successfully", response));
    }

    @PostMapping("/return")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<ApiResponse<ReturnResponseDto>> processReturn(
            @Valid @RequestBody ReturnRequestDto dto,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String xIdempotencyKey,
            @AuthenticationPrincipal UserPrincipal staffUser) {

        String effectiveKey = idempotencyKey != null ? idempotencyKey : xIdempotencyKey;
        ReturnResponseDto response = circulationOrchestrator.processReturn(dto, staffUser.getId(), effectiveKey);
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }

    @GetMapping("/records")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<ApiResponse<Page<BorrowingRecordResponseDto>>> getAllRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<BorrowingRecordResponseDto> records = borrowingService.getAllRecords(pageable);
        return ResponseEntity.ok(ApiResponse.success(records));
    }
}
