package com.borrowhub.controller;

import com.borrowhub.payload.request.WaitlistJoinRequest;
import com.borrowhub.payload.response.ApiResponse;
import com.borrowhub.payload.response.WaitlistEntryResponseDto;
import com.borrowhub.security.UserPrincipal;
import com.borrowhub.service.WaitlistService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/waitlist")
public class WaitlistController {

    private final WaitlistService waitlistService;

    public WaitlistController(WaitlistService waitlistService) {
        this.waitlistService = waitlistService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WaitlistEntryResponseDto>> joinWaitlist(
            @Valid @RequestBody WaitlistJoinRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        WaitlistEntryResponseDto response = waitlistService.joinWaitlist(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Successfully joined the queue", response));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<WaitlistEntryResponseDto>>> getMyWaitlist(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<WaitlistEntryResponseDto> list = waitlistService.getMyWaitlist(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelWaitlist(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        waitlistService.cancelWaitlist(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Waitlist entry cancelled", null));
    }
}
