package com.borrowhub.controller;

import com.borrowhub.exception.BadRequestException;
import com.borrowhub.payload.request.ReservationCreateDto;
import com.borrowhub.payload.response.ApiResponse;
import com.borrowhub.payload.response.ReservationResponseDto;
import com.borrowhub.security.UserPrincipal;
import com.borrowhub.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReservationResponseDto>> createReservation(
            @Valid @RequestBody ReservationCreateDto dto,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        ReservationResponseDto response = reservationService.createReservation(currentUser.getId(), dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Reservation confirmed and held successfully", response));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<ReservationResponseDto>>> getMyReservations(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<ReservationResponseDto> list = reservationService.getMyReservations(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReservationResponseDto>> getReservationById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        ReservationResponseDto response = reservationService.getReservationById(id);
        boolean isOwner = response.getUserId().equals(currentUser.getId());
        boolean isElevated = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_FACULTY")
                        || a.getAuthority().equals("ROLE_STAFF"));
        if (!isOwner && !isElevated) {
            throw new BadRequestException("You are not authorized to view this reservation");
        }
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelReservation(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        String reason = body != null ? body.get("reason") : "Cancelled by user";
        reservationService.cancelReservation(id, currentUser.getId(), reason);
        return ResponseEntity.ok(ApiResponse.success("Reservation cancelled successfully", null));
    }
}
