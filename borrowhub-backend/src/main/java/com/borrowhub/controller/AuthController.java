package com.borrowhub.controller;

import com.borrowhub.model.entity.User;
import com.borrowhub.payload.request.LoginRequest;
import com.borrowhub.payload.request.RegisterRequest;
import com.borrowhub.payload.response.ApiResponse;
import com.borrowhub.payload.response.AuthResponse;
import com.borrowhub.payload.response.UserProfileResponse;
import com.borrowhub.security.UserPrincipal;
import com.borrowhub.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        AuthResponse response = authService.authenticate(loginRequest);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        User registeredUser = authService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully with id: " + registeredUser.getId(), null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("User not authenticated"));
        }
        UserProfileResponse profile = authService.getProfile(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }
}
