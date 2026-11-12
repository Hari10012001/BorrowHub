package com.borrowhub.service;

import com.borrowhub.exception.BadRequestException;
import com.borrowhub.exception.ResourceNotFoundException;
import com.borrowhub.model.entity.User;
import com.borrowhub.model.enums.AccountStatus;
import com.borrowhub.model.enums.BorrowingStatus;
import com.borrowhub.model.enums.ReservationStatus;
import com.borrowhub.model.enums.Role;
import com.borrowhub.payload.request.LoginRequest;
import com.borrowhub.payload.request.RegisterRequest;
import com.borrowhub.payload.response.AuthResponse;
import com.borrowhub.payload.response.UserProfileResponse;
import com.borrowhub.repository.BorrowingRecordRepository;
import com.borrowhub.repository.ReservationRepository;
import com.borrowhub.repository.UserRepository;
import com.borrowhub.security.JwtTokenProvider;
import com.borrowhub.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final BorrowingRecordRepository borrowingRecordRepository;
    private final ReservationRepository reservationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       BorrowingRecordRepository borrowingRecordRepository,
                       ReservationRepository reservationRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.borrowingRecordRepository = borrowingRecordRepository;
        this.reservationRepository = reservationRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    public AuthResponse authenticate(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return new AuthResponse(
                jwt,
                userPrincipal.getId(),
                userPrincipal.getUsername(),
                userPrincipal.getEmail(),
                userPrincipal.getFullName(),
                userPrincipal.getRole()
        );
    }

    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username is already taken!");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered!");
        }

        String instId = request.getInstitutionalId();
        if (instId == null || instId.isBlank()) {
            instId = "STU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }

        User user = new User();
        user.setUsername(request.getUsername().trim().toLowerCase());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setDepartment(request.getDepartment() != null ? request.getDepartment() : "General");
        user.setPhone(request.getPhone());
        user.setInstitutionalId(instId);
        // Security guard: Public self-registration can NEVER assign privileged roles (ADMIN)
        Role assignedRole = request.getRole();
        if (assignedRole == null || assignedRole == Role.ADMIN) {
            assignedRole = Role.STUDENT;
        }
        user.setRole(assignedRole);
        user.setAccountStatus(AccountStatus.ACTIVE);

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        List<BorrowingStatus> activeBorrowingStatuses = Arrays.asList(
                BorrowingStatus.ACTIVE,
                BorrowingStatus.PARTIALLY_RETURNED,
                BorrowingStatus.OVERDUE,
                BorrowingStatus.RETURN_PENDING
        );
        long activeBorrowings = borrowingRecordRepository.countByUserIdAndStatusIn(userId, activeBorrowingStatuses);

        List<ReservationStatus> activeReservationStatuses = List.of(ReservationStatus.HELD);
        long activeReservations = reservationRepository.countByUserIdAndStatusIn(userId, activeReservationStatuses);

        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setFullName(user.getFullName());
        response.setRole(user.getRole().name());
        response.setAccountStatus(user.getAccountStatus().name());
        response.setDepartment(user.getDepartment());
        response.setPhone(user.getPhone());
        response.setInstitutionalId(user.getInstitutionalId());
        response.setCreatedAt(user.getCreatedAt());
        response.setActiveBorrowingsCount(activeBorrowings);
        response.setActiveReservationsCount(activeReservations);

        return response;
    }
}
