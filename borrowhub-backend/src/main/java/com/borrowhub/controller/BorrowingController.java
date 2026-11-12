package com.borrowhub.controller;

import com.borrowhub.payload.request.BorrowingApprovalDto;
import com.borrowhub.payload.request.BorrowingRequestCreateDto;
import com.borrowhub.payload.response.ApiResponse;
import com.borrowhub.payload.response.BorrowingRecordResponseDto;
import com.borrowhub.payload.response.BorrowingRequestResponseDto;
import com.borrowhub.security.UserPrincipal;
import com.borrowhub.service.BorrowingRequestService;
import com.borrowhub.service.BorrowingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/borrowings")
public class BorrowingController {

    private final BorrowingRequestService borrowingRequestService;
    private final BorrowingService borrowingService;

    public BorrowingController(BorrowingRequestService borrowingRequestService,
                               BorrowingService borrowingService) {
        this.borrowingRequestService = borrowingRequestService;
        this.borrowingService = borrowingService;
    }

    @PostMapping("/requests")
    public ResponseEntity<ApiResponse<BorrowingRequestResponseDto>> submitRequest(
            @Valid @RequestBody BorrowingRequestCreateDto dto,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        BorrowingRequestResponseDto response = borrowingRequestService.submitRequest(currentUser.getId(), dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Borrowing request submitted successfully", response));
    }

    @GetMapping("/requests/my")
    public ResponseEntity<ApiResponse<List<BorrowingRequestResponseDto>>> getMyRequests(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<BorrowingRequestResponseDto> list = borrowingRequestService.getMyRequests(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/requests/pending")
    @PreAuthorize("hasAnyRole('FACULTY', 'STAFF', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<BorrowingRequestResponseDto>>> getPendingRequests() {
        List<BorrowingRequestResponseDto> list = borrowingRequestService.getPendingRequests();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PostMapping("/requests/{id}/review")
    @PreAuthorize("hasAnyRole('FACULTY', 'STAFF', 'ADMIN')")
    public ResponseEntity<ApiResponse<BorrowingRequestResponseDto>> reviewRequest(
            @PathVariable Long id,
            @Valid @RequestBody BorrowingApprovalDto dto,
            @AuthenticationPrincipal UserPrincipal reviewer) {
        BorrowingRequestResponseDto response = borrowingRequestService.reviewRequest(id, reviewer.getId(), dto);
        return ResponseEntity.ok(ApiResponse.success("Request reviewed successfully", response));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<BorrowingRecordResponseDto>>> getMyBorrowings(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<BorrowingRecordResponseDto> records = borrowingService.getMyBorrowings(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(records));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BorrowingRecordResponseDto>> getBorrowingRecord(@PathVariable Long id) {
        BorrowingRecordResponseDto record = borrowingService.getRecordById(id);
        return ResponseEntity.ok(ApiResponse.success(record));
    }
}
