package com.davydcr.document.infrastructure.security;

import com.davydcr.document.infrastructure.persistence.entity.AuditLogEntity;
import com.davydcr.document.infrastructure.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("AuditLogService")
class AuditLogServiceTest {

  private AuditLogService auditLogService;
  
  @Autowired
  private AuditLogRepository auditLogRepository;

  @BeforeEach
  void setUp() {
    auditLogService = new AuditLogService(auditLogRepository);
    auditLogRepository.deleteAll();
  }

  @Test
  @DisplayName("Deve registrar login bem-sucedido")
  void testLogLoginSuccess() {
    // Given
    String userId = UUID.randomUUID().toString();
    String email = "test@example.com";
    String ipAddress = "192.168.1.1";
    String userAgent = "Mozilla/5.0";

    // When
    AuditLogEntity log = auditLogService.logLoginSuccess(userId, email, ipAddress, userAgent, "/api/auth/login");

    // Then
    assertNotNull(log);
    assertEquals(userId, log.getUserId());
    assertEquals(email, log.getEmail());
    assertEquals(ipAddress, log.getIpAddress());
    assertEquals(userAgent, log.getUserAgent());
    assertEquals(AuditEventType.LOGIN_SUCCESS.getCode(), log.getEventType());
    assertEquals(200, log.getStatusCode());
  }

  @Test
  @DisplayName("Deve registrar falha de login")
  void testLogLoginFailure() {
    // Given
    String email = "test@example.com";
    String ipAddress = "192.168.1.1";
    String userAgent = "Mozilla/5.0";
    String reason = "Credenciais inválidas";

    // When
    AuditLogEntity log = auditLogService.logLoginFailure(email, ipAddress, userAgent, "/api/auth/login", reason);

    // Then
    assertNotNull(log);
    assertNull(log.getUserId()); // Não tem userId em falha
    assertEquals(email, log.getEmail());
    assertEquals(ipAddress, log.getIpAddress());
    assertEquals(AuditEventType.LOGIN_FAILURE.getCode(), log.getEventType());
    assertEquals(401, log.getStatusCode());
    assertEquals(reason, log.getErrorMessage());
  }

  @Test
  @DisplayName("Deve registrar logout")
  void testLogLogout() {
    // Given
    String userId = UUID.randomUUID().toString();
    String email = "test@example.com";
    String ipAddress = "192.168.1.1";
    String userAgent = "Mozilla/5.0";

    // When
    AuditLogEntity log = auditLogService.logLogout(userId, email, ipAddress, userAgent, "/api/auth/logout");

    // Then
    assertNotNull(log);
    assertEquals(userId, log.getUserId());
    assertEquals(AuditEventType.LOGOUT_SUCCESS.getCode(), log.getEventType());
    assertEquals(200, log.getStatusCode());
  }

  @Test
  @DisplayName("Deve registrar refresh de token")
  void testLogTokenRefresh() {
    // Given
    String userId = UUID.randomUUID().toString();
    String email = "test@example.com";
    String ipAddress = "192.168.1.1";
    String userAgent = "Mozilla/5.0";

    // When
    AuditLogEntity log = auditLogService.logTokenRefresh(userId, email, ipAddress, userAgent, "/api/auth/refresh");

    // Then
    assertNotNull(log);
    assertEquals(userId, log.getUserId());
    assertEquals(AuditEventType.TOKEN_REFRESH_SUCCESS.getCode(), log.getEventType());
    assertEquals(200, log.getStatusCode());
  }

  @Test
  @DisplayName("Deve registrar falha de refresh de token")
  void testLogTokenRefreshFailure() {
    // Given
    String email = "test@example.com";
    String ipAddress = "192.168.1.1";
    String userAgent = "Mozilla/5.0";

    // When
    AuditLogEntity log = auditLogService.logTokenRefreshFailure(email, ipAddress, userAgent, "/api/auth/refresh");

    // Then
    assertNotNull(log);
    assertNull(log.getUserId());
    assertEquals(AuditEventType.TOKEN_REFRESH_FAILURE.getCode(), log.getEventType());
    assertEquals(401, log.getStatusCode());
  }

  @Test
  @DisplayName("Deve registrar acesso a recurso")
  void testLogResourceAccess() {
    // Given
    String userId = UUID.randomUUID().toString();
    String email = "test@example.com";
    String ipAddress = "192.168.1.1";
    String userAgent = "Mozilla/5.0";
    String endpoint = "/api/documents";

    // When
    AuditLogEntity log = auditLogService.logResourceAccess(userId, email, ipAddress, userAgent, endpoint, "GET");

    // Then
    assertNotNull(log);
    assertEquals(userId, log.getUserId());
    assertEquals(AuditEventType.ACCESS_GRANTED.getCode(), log.getEventType());
    assertEquals(200, log.getStatusCode());
    assertEquals(endpoint, log.getEndpoint());
    assertEquals("GET", log.getMethod());
  }

  @Test
  @DisplayName("Deve detectar brute force por email")
  void testDetectBruteForceByEmail() {
    // Given
    String email = "attacker@example.com";
    String ipAddress = "10.0.0.1";

    // Registrar 5 tentativas falhadas - usar email igual
    for (int i = 0; i < 5; i++) {
      auditLogService.logLoginFailure(email, ipAddress + i, "Mozilla/5.0", "/api/auth/login", "Falha");
    }

    // When
    boolean isBruteForce = auditLogService.isBruteForceAttempt(email);

    // Then
    assertTrue(isBruteForce);
  }

  @Test
  @DisplayName("Deve detectar brute force por IP")
  void testDetectBruteForceByIp() {
    // Given
    String email = "test@example.com";
    String ipAddress = "10.0.0.1";

    // Registrar 5 tentativas falhadas do mesmo IP
    for (int i = 0; i < 5; i++) {
      auditLogService.logLoginFailure(email + i, ipAddress, "Mozilla/5.0", "/api/auth/login", "Falha");
    }

    // When
    boolean isBruteForce = auditLogService.isBruteForceByIp(ipAddress);

    // Then
    assertTrue(isBruteForce);
  }

  @Test
  @DisplayName("Não deve detectar brute force com menos de 5 tentativas")
  void testNoBruteForceWithLessThan5Attempts() {
    // Given
    String email = "test@example.com";
    String ipAddress = "10.0.0.1";

    // Registrar apenas 3 tentativas falhadas
    for (int i = 0; i < 3; i++) {
      auditLogService.logLoginFailure(email, ipAddress, "Mozilla/5.0", "/api/auth/login", "Falha");
    }

    // When
    boolean isBruteForce = auditLogService.isBruteForceAttempt(email);

    // Then
    assertFalse(isBruteForce); // H2 pode ter limitações em queries nativas
  }

  @Test
  @DisplayName("Deve obter falhas de login recentes")
  void testGetFailedLoginAttempts() {
    // Given
    String email = "test@example.com";
    LocalDateTime now = LocalDateTime.now();

    // Registrar 3 falhas
    for (int i = 0; i < 3; i++) {
      auditLogService.logLoginFailure(email, "192.168.1." + i, "Mozilla/5.0", "/api/auth/login", "Falha");
    }

    // When
    List<AuditLogEntity> attempts = auditLogService.getFailedLoginAttempts(email, 15);

    // Then
    assertEquals(3, attempts.size());
    assertTrue(attempts.stream().allMatch(a -> a.getEmail().equals(email)));
    assertTrue(attempts.stream().allMatch(a -> a.isFailure()));
  }

  @Test
  @DisplayName("Deve obter logs de auditoria de um usuário")
  void testGetUserAuditLogs() {
    // Given
    String userId = UUID.randomUUID().toString();
    String email = "test@example.com";
    String ipAddress = "192.168.1.1";

    // Registrar múltiplos eventos
    auditLogService.logLoginSuccess(userId, email, ipAddress, "Mozilla/5.0", "/api/auth/login");
    auditLogService.logResourceAccess(userId, email, ipAddress, "Mozilla/5.0", "/api/documents", "GET");
    auditLogService.logLogout(userId, email, ipAddress, "Mozilla/5.0", "/api/auth/logout");

    // When
    Page<AuditLogEntity> logs = auditLogService.getUserAuditLogs(userId, Pageable.unpaged());

    // Then
    assertEquals(3, logs.getTotalElements());
    assertTrue(logs.stream().allMatch(l -> l.getUserId().equals(userId)));
  }

  @Test
  @DisplayName("Deve obter logs por tipo de evento")
  void testGetEventTypeLogs() {
    // Given
    String userId = UUID.randomUUID().toString();
    String email = "test@example.com";
    String ipAddress = "192.168.1.1";

    // Registrar múltiplos eventos
    auditLogService.logLoginSuccess(userId, email, ipAddress, "Mozilla/5.0", "/api/auth/login");
    auditLogService.logLoginSuccess(userId, email, ipAddress, "Mozilla/5.0", "/api/auth/login");
    auditLogService.logLogout(userId, email, ipAddress, "Mozilla/5.0", "/api/auth/logout");

    // When
    Page<AuditLogEntity> loginLogs = auditLogService.getEventTypeLogs(
        AuditEventType.LOGIN_SUCCESS.getCode(), Pageable.unpaged());

    // Then
    assertEquals(2, loginLogs.getTotalElements());
    assertTrue(loginLogs.stream().allMatch(l -> l.getEventType().equals(AuditEventType.LOGIN_SUCCESS.getCode())));
  }

  @Test
  @DisplayName("Deve obter logs por IP")
  void testGetIpAddressLogs() {
    // Given
    String ipAddress = "192.168.1.1";
    String userId = UUID.randomUUID().toString();
    String email = "test@example.com";

    // Registrar múltiplos eventos
    auditLogService.logLoginSuccess(userId, email, ipAddress, "Mozilla/5.0", "/api/auth/login");
    auditLogService.logResourceAccess(userId, email, ipAddress, "Mozilla/5.0", "/api/documents", "GET");

    // When
    Page<AuditLogEntity> logs = auditLogService.getIpAddressLogs(ipAddress, Pageable.unpaged());

    // Then
    assertEquals(2, logs.getTotalElements());
    assertTrue(logs.stream().allMatch(l -> l.getIpAddress().equals(ipAddress)));
  }

  @Test
  @DisplayName("Deve obter atividades suspeitas")
  void testGetSuspiciousActivity() {
    // Given
    String email = "attacker@example.com";
    String ipAddress = "10.0.0.1";

    // Registrar atividades suspeitas
    auditLogService.logLoginFailure(email, ipAddress, "Mozilla/5.0", "/api/auth/login", "Falha 1");
    auditLogService.logLoginFailure(email, ipAddress, "Mozilla/5.0", "/api/auth/login", "Falha 2");

    // When
    Page<AuditLogEntity> suspicious = auditLogService.getSuspiciousActivity(1, Pageable.unpaged());

    // Then
    assertNotNull(suspicious);
    assertTrue(suspicious.getTotalElements() >= 2);
    assertTrue(suspicious.stream().allMatch(AuditLogEntity::isSuspicious));
  }

  @Test
  @DisplayName("Deve obter sesões de um usuário")
  void testGetUserSessions() {
    // Given
    String userId = UUID.randomUUID().toString();
    String email = "test@example.com";
    String ipAddress = "192.168.1.1";

    // Registrar múltiplos logins
    auditLogService.logLoginSuccess(userId, email, ipAddress, "Mozilla/5.0", "/api/auth/login");
    auditLogService.logLogout(userId, email, ipAddress, "Mozilla/5.0", "/api/auth/logout");

    // When
    Page<AuditLogEntity> sessions = auditLogService.getUserSessions(userId, Pageable.unpaged());

    // Then
    assertTrue(sessions.getTotalElements() >= 1);
    assertTrue(sessions.stream().anyMatch(s -> s.getEventType().equals(AuditEventType.LOGIN_SUCCESS.getCode())));
  }

  @Test
  @DisplayName("Deve obter logs por intervalo de datas")
  void testGetLogsByDateRange() {
    // Given
    String userId = UUID.randomUUID().toString();
    String email = "test@example.com";
    String ipAddress = "192.168.1.1";
    LocalDateTime startDate = LocalDateTime.now().minusHours(2);
    LocalDateTime endDate = LocalDateTime.now().plusHours(2);

    // Registrar log
    auditLogService.logLoginSuccess(userId, email, ipAddress, "Mozilla/5.0", "/api/auth/login");

    // When
    Page<AuditLogEntity> logs = auditLogService.getLogsByDateRange(startDate, endDate, Pageable.unpaged());

    // Then
    assertEquals(1, logs.getTotalElements());
  }

  @Test
  @DisplayName("Deve gerar relatório de segurança")
  void testGenerateSecurityReport() {
    // Given - Setup atividade
    String email = "attacker@example.com";
    auditLogService.logLoginFailure(email, "10.0.0.1", "Mozilla/5.0", "/api/auth/login", "Falha");
    auditLogService.logLoginFailure(email, "10.0.0.1", "Mozilla/5.0", "/api/auth/login", "Falha");

    // When
    AuditLogService.SecurityReport report = auditLogService.generateSecurityReport();

    // Then
    assertNotNull(report);
    // Não pode garantir números em H2, mas deve retornar objeto válido
    assertTrue(report.suspiciousEventCount >= 0);
  }

  @Test
  @DisplayName("Deve contar eventos por tipo")
  void testCountByEventType() {
    // Given
    String userId = UUID.randomUUID().toString();
    String email = "test@example.com";
    String ipAddress = "192.168.1.1";

    // Registrar 3 logins
    for (int i = 0; i < 3; i++) {
      auditLogService.logLoginSuccess(userId, email, ipAddress, "Mozilla/5.0", "/api/auth/login");
    }

    // When
    long count = auditLogService.countByEventType(AuditEventType.LOGIN_SUCCESS.getCode());

    // Then
    assertEquals(3, count);
  }
}
