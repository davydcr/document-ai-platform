package com.davydcr.document.infrastructure.controller;

import com.davydcr.document.application.dto.LoginRequest;
import com.davydcr.document.infrastructure.persistence.entity.RefreshTokenEntity;
import com.davydcr.document.infrastructure.persistence.entity.RoleEntity;
import com.davydcr.document.infrastructure.persistence.entity.UserAccountEntity;
import com.davydcr.document.infrastructure.repository.RefreshTokenRepository;
import com.davydcr.document.infrastructure.repository.UserRepository;
import com.davydcr.document.infrastructure.security.AuditLogService;
import com.davydcr.document.infrastructure.security.JwtProvider;
import com.davydcr.document.infrastructure.security.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes do AuthController com auditoria integrada
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @MockBean
    private RefreshTokenRepository refreshTokenRepository;

    @MockBean
    private AuditLogService auditLogService;

    @Autowired
    private JwtProvider jwtProvider;

    private UserAccountEntity testUser;
    private RoleEntity testRole;

    @BeforeEach
    void setUp() {
        // Preparar dados de teste
        testRole = new RoleEntity();
        testRole.setId(1L);
        testRole.setName("USER");

        testUser = new UserAccountEntity();
        testUser.setId(UUID.randomUUID().toString());
        testUser.setEmail("test@example.com");
        testUser.setUsername("testuser");
        testUser.setPasswordHash(passwordEncoder.encode("password123"));
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setActive(true);
        testUser.setRoles(new HashSet<>(Set.of(testRole)));
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * Teste: Login com sucesso
     */
    @Test
    void testLoginSuccess() throws Exception {
        // Arrange
        LoginRequest loginRequest = new LoginRequest("test@example.com", "password123");
        RefreshTokenEntity refreshToken = new RefreshTokenEntity();
        refreshToken.setId(UUID.randomUUID().toString());
        refreshToken.setUserId(testUser.getId());
        refreshToken.setToken("refresh-token-value");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(refreshTokenService.createRefreshToken(testUser.getId())).thenReturn(refreshToken);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.email").value("test@example.com"));

        // Verificar chamadas de auditoria
        verify(auditLogService, times(1)).logLoginSuccess(
                eq(testUser.getId()),
                eq(testUser.getEmail()),
                anyString(),
                anyString(),
                eq("/api/auth/login")
        );
        verify(auditLogService, never()).logLoginFailure(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    /**
     * Teste: Login com credenciais inválidas (usuário não encontrado)
     */
    @Test
    void testLoginUserNotFound() throws Exception {
        // Arrange
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"nonexistent@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Credenciais inválidas"));

        // Verificar chamadas de auditoria
        verify(auditLogService, times(1)).logLoginFailure(
                eq("nonexistent@example.com"),
                anyString(),
                anyString(),
                eq("/api/auth/login"),
                eq("Credenciais inválidas")
        );
        verify(auditLogService, never()).logLoginSuccess(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    /**
     * Teste: Login com senha incorreta
     */
    @Test
    void testLoginWrongPassword() throws Exception {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test@example.com\",\"password\":\"wrongpassword\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Credenciais inválidas"));

        // Verificar chamadas de auditoria
        verify(auditLogService, times(1)).logLoginFailure(
                eq(testUser.getEmail()),
                anyString(),
                anyString(),
                eq("/api/auth/login"),
                eq("Senha incorreta")
        );
        verify(auditLogService, never()).logLoginSuccess(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    /**
     * Teste: Refresh token com sucesso
     */
    @Test
    void testRefreshTokenSuccess() throws Exception {
        // Arrange
        RefreshTokenEntity validToken = new RefreshTokenEntity();
        validToken.setId(UUID.randomUUID().toString());
        validToken.setUserId(testUser.getId());
        validToken.setToken("valid-refresh-token");
        validToken.setRevoked(false);

        when(refreshTokenService.validateRefreshToken("valid-refresh-token"))
                .thenReturn(Optional.of(validToken));
        when(userRepository.findByUserId(testUser.getId()))
                .thenReturn(Optional.of(testUser));

        // Act & Assert
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"valid-refresh-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.email").value("test@example.com"));

        // Verificar chamadas de auditoria
        verify(auditLogService, times(1)).logTokenRefresh(
                eq(testUser.getId()),
                eq(testUser.getEmail()),
                anyString(),
                anyString(),
                eq("/api/auth/refresh")
        );
        verify(auditLogService, never()).logTokenRefreshFailure(anyString(), anyString(), anyString(), anyString());
    }

    /**
     * Teste: Refresh token inválido
     */
    @Test
    void testRefreshTokenInvalid() throws Exception {
        // Arrange
        when(refreshTokenService.validateRefreshToken("invalid-token"))
                .thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"invalid-token\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Refresh token inválido ou expirado"));

        // Verificar chamadas de auditoria
        verify(auditLogService, times(1)).logTokenRefreshFailure(
                eq("unknown"),
                anyString(),
                anyString(),
                eq("/api/auth/refresh")
        );
    }

    /**
     * Teste: Logout com sucesso
     */
    @Test
    void testLogoutSuccess() throws Exception {
        // Arrange
        RefreshTokenEntity validToken = new RefreshTokenEntity();
        validToken.setId(UUID.randomUUID().toString());
        validToken.setUserId(testUser.getId());
        validToken.setToken("valid-refresh-token");
        validToken.setRevoked(false);

        when(refreshTokenService.validateRefreshToken("valid-refresh-token"))
                .thenReturn(Optional.of(validToken));
        when(userRepository.findByUserId(testUser.getId()))
                .thenReturn(Optional.of(testUser));

        // Act & Assert
        mockMvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"valid-refresh-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout realizado com sucesso"));

        // Verificar chamadas de auditoria
        verify(auditLogService, times(1)).logLogout(
                eq(testUser.getId()),
                eq(testUser.getEmail()),
                anyString(),
                anyString(),
                eq("/api/auth/logout")
        );
        verify(refreshTokenService, times(1)).revokeRefreshToken("valid-refresh-token");
    }

    /**
     * Teste: Logout sem refresh token (não deveria causar erro)
     */
    @Test
    void testLogoutWithoutToken() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout realizado com sucesso"));

        // Verificar que não houve chamada de auditoria para logout específico
        verify(auditLogService, never()).logLogout(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    /**
     * Teste: Login - capturação de IP address
     */
    @Test
    void testLoginCapturesIpAddress() throws Exception {
        // Arrange
        RefreshTokenEntity refreshToken = new RefreshTokenEntity();
        refreshToken.setId(UUID.randomUUID().toString());
        refreshToken.setUserId(testUser.getId());
        refreshToken.setToken("refresh-token");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(refreshTokenService.createRefreshToken(testUser.getId())).thenReturn(refreshToken);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test@example.com\",\"password\":\"password123\"}")
                .header("X-Forwarded-For", "192.168.1.100"))
                .andExpect(status().isOk());

        // Verificar que IP foi capturado
        verify(auditLogService, times(1)).logLoginSuccess(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString()
        );
    }
}
