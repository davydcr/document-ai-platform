package com.davydcr.document.infrastructure;

import com.davydcr.document.application.dto.LoginRequest;
import com.davydcr.document.infrastructure.controller.AuthController;
import com.davydcr.document.infrastructure.persistence.entity.RoleEntity;
import com.davydcr.document.infrastructure.persistence.entity.UserAccountEntity;
import com.davydcr.document.infrastructure.repository.UserRepository;
import com.davydcr.document.infrastructure.security.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UserAccountEntity testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        // Create test user
        testUser = new UserAccountEntity();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setActive(true);
        testUser.setPasswordHash(passwordEncoder.encode("password123"));

        // Create and assign role
        RoleEntity userRole = new RoleEntity("USER", "Standard user access");
        testUser.setRoles(Set.of(userRole));

        userRepository.save(testUser);
    }

    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/auth/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"));
    }

    @Test
    void testLoginWithValidCredentials() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType("application/json")
                .content("{\"username\": \"test@example.com\", \"password\": \"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.username").value("test@example.com"))
                .andExpect(jsonPath("$.expiresIn").exists());
    }

    @Test
    void testLoginWithInvalidPassword() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType("application/json")
                .content("{\"username\": \"test@example.com\", \"password\": \"wrongpassword\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Credenciais inválidas"));
    }

    @Test
    void testLoginWithNonexistentUser() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType("application/json")
                .content("{\"username\": \"nonexistent@example.com\", \"password\": \"password123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Credenciais inválidas"));
    }

    @Test
    void testLoginWithInactiveUser() throws Exception {
        testUser.setActive(false);
        userRepository.save(testUser);

        mockMvc.perform(post("/auth/login")
                .contentType("application/json")
                .content("{\"username\": \"test@example.com\", \"password\": \"password123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Credenciais inválidas"));
    }
}
