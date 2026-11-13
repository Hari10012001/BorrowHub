package com.borrowhub.controller;

import com.borrowhub.model.enums.Role;
import com.borrowhub.payload.request.LoginRequest;
import com.borrowhub.payload.request.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should successfully authenticate valid student credentials and return JWT")
    void testValidStudentLogin() throws Exception {
        String studentPassword = System.getenv("BORROWHUB_SEED_STUDENT_PASSWORD");
        if (studentPassword == null || studentPassword.isBlank()) {
            studentPassword = "Student@123";
        }

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("student1");
        loginRequest.setPassword(studentPassword);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.username").value("student1"))
                .andExpect(jsonPath("$.data.role").value("STUDENT"));
    }

    @Test
    @DisplayName("Should reject invalid password with 401 Unauthorized")
    void testInvalidPasswordRejected() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("student1");
        loginRequest.setPassword("WrongPassword@999");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should reject invalid request body with 400 Bad Request")
    void testValidationFailure() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(""); // blank username
        loginRequest.setPassword("");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should register a new student user successfully with 201 Created")
    void testStudentRegistration() throws Exception {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser_" + uniqueSuffix);
        registerRequest.setEmail("newuser_" + uniqueSuffix + "@borrowhub.edu");
        registerRequest.setPassword("SecurePass@123");
        registerRequest.setFirstName("New");
        registerRequest.setLastName("User" + uniqueSuffix);
        registerRequest.setDepartment("Computer Science");
        registerRequest.setPhone("+919876543210");
        registerRequest.setRole(Role.STUDENT);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Security Check: /api/auth/me should return 401 when token is missing")
    void testGetProfileUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Security Check: Public catalog should be accessible without credentials")
    void testPublicCatalogAccessible() throws Exception {
        mockMvc.perform(get("/api/resources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Security Check: Admin pending approvals should reject unauthenticated requests with 401")
    void testPendingApprovalsRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/borrowings/requests/pending"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Security Regression Check: Registration with ADMIN role must be demoted to STUDENT")
    void testPrivilegeEscalationPrevented() throws Exception {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("hacker_" + uniqueSuffix);
        registerRequest.setEmail("hacker_" + uniqueSuffix + "@borrowhub.edu");
        registerRequest.setPassword("SecurePass@123");
        registerRequest.setFirstName("Privilege");
        registerRequest.setLastName("Escalation");
        registerRequest.setRole(Role.ADMIN);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Now attempt login and verify assigned role is STUDENT, NOT ADMIN
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("hacker_" + uniqueSuffix);
        loginRequest.setPassword("SecurePass@123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("STUDENT"));
    }
}
