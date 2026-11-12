package com.borrowhub.payload.response;

import java.time.Instant;

public class UserProfileResponse {

    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private String role;
    private String accountStatus;
    private String department;
    private String phone;
    private String institutionalId;
    private Instant createdAt;
    private long activeBorrowingsCount;
    private long activeReservationsCount;

    public UserProfileResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getAccountStatus() { return accountStatus; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getInstitutionalId() { return institutionalId; }
    public void setInstitutionalId(String institutionalId) { this.institutionalId = institutionalId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public long getActiveBorrowingsCount() { return activeBorrowingsCount; }
    public void setActiveBorrowingsCount(long activeBorrowingsCount) { this.activeBorrowingsCount = activeBorrowingsCount; }

    public long getActiveReservationsCount() { return activeReservationsCount; }
    public void setActiveReservationsCount(long activeReservationsCount) { this.activeReservationsCount = activeReservationsCount; }
}
