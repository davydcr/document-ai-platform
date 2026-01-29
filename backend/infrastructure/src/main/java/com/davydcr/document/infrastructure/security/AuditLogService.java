package com.davydcr.document.infrastructure.security;

import com.davydcr.document.infrastructure.persistence.entity.AuditLogEntity;
import com.davydcr.document.infrastructure.repository.AuditLogRepository;
import com.davydcr.document.infrastructure.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Serviço de auditoria para logging de eventos de autenticação e acesso.
 * 
 * Responsibilidades:
 * - Registrar eventos de login/logout
 * - Registrar tentativas de acesso
 * - Detectar atividades suspeitas
 * - Limpeza de logs antigos
 * - Análise de padrões de segurança
 * - Enviar alertas de segurança por email
 */
@Service
public class AuditLogService {

  private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);
  private static final int FAILED_LOGIN_THRESHOLD = 5;
  private static final int FAILED_LOGIN_WINDOW_MINUTES = 15;
  private static final int LOG_RETENTION_DAYS = 90;

  private final AuditLogRepository auditLogRepository;
  private final EmailService emailService;

  public AuditLogService(AuditLogRepository auditLogRepository, @org.springframework.beans.factory.annotation.Autowired(required = false) EmailService emailService) {
    this.auditLogRepository = auditLogRepository;
    this.emailService = emailService;
  }

  /**
   * Registra um evento de auditoria
   */
  @Transactional
  public AuditLogEntity logEvent(String eventType, String email, String ipAddress, 
                                 String userAgent, String endpoint, String method) {
    return logEvent(eventType, null, email, ipAddress, userAgent, endpoint, method, 200, null, null);
  }

  /**
   * Registra um evento de auditoria com detalhes completos
   */
  @Transactional
  public AuditLogEntity logEvent(String eventType, String userId, String email, String ipAddress,
                                 String userAgent, String endpoint, String method,
                                 Integer statusCode, String errorMessage, String details) {
    String id = UUID.randomUUID().toString();

    AuditLogEntity log = new AuditLogEntity.Builder(id, eventType, email, ipAddress)
        .userId(userId)
        .userAgent(userAgent)
        .endpoint(endpoint)
        .method(method)
        .statusCode(statusCode)
        .errorMessage(errorMessage)
        .details(details)
        .build();

    AuditLogEntity saved = auditLogRepository.save(log);
    logger.info("Audit log created: {} - {} - {} - {}", eventType, email, ipAddress, statusCode);
    return saved;
  }

  /**
   * Registra um login bem-sucedido
   */
  @Transactional
  public AuditLogEntity logLoginSuccess(String userId, String email, String ipAddress, 
                                        String userAgent, String endpoint) {
    return logEvent(AuditEventType.LOGIN_SUCCESS.getCode(), userId, email, ipAddress,
        userAgent, endpoint, "POST", 200, null, null);
  }

  /**
   * Registra uma tentativa de login falhada
   */
  @Transactional
  public AuditLogEntity logLoginFailure(String email, String ipAddress, String userAgent,
                                        String endpoint, String errorReason) {
    return logEvent(AuditEventType.LOGIN_FAILURE.getCode(), null, email, ipAddress,
        userAgent, endpoint, "POST", 401, errorReason, null);
  }

  /**
   * Registra um logout
   */
  @Transactional
  public AuditLogEntity logLogout(String userId, String email, String ipAddress,
                                  String userAgent, String endpoint) {
    return logEvent(AuditEventType.LOGOUT_SUCCESS.getCode(), userId, email, ipAddress,
        userAgent, endpoint, "POST", 200, null, null);
  }

  /**
   * Registra um refresh de token
   */
  @Transactional
  public AuditLogEntity logTokenRefresh(String userId, String email, String ipAddress,
                                        String userAgent, String endpoint) {
    return logEvent(AuditEventType.TOKEN_REFRESH_SUCCESS.getCode(), userId, email, ipAddress,
        userAgent, endpoint, "POST", 200, null, null);
  }

  /**
   * Registra uma tentativa de refresh de token inválido
   */
  @Transactional
  public AuditLogEntity logTokenRefreshFailure(String email, String ipAddress,
                                               String userAgent, String endpoint) {
    return logEvent(AuditEventType.TOKEN_REFRESH_FAILURE.getCode(), null, email, ipAddress,
        userAgent, endpoint, "POST", 401, "Token inválido ou expirado", null);
  }

  /**
   * Registra acesso a recurso
   */
  @Transactional
  public AuditLogEntity logResourceAccess(String userId, String email, String ipAddress,
                                          String userAgent, String endpoint, String method) {
    return logEvent(AuditEventType.ACCESS_GRANTED.getCode(), userId, email, ipAddress,
        userAgent, endpoint, method, 200, null, null);
  }

  /**
   * Verifica se há múltiplas tentativas de login falhadas (detecção de brute force)
   */
  public boolean isBruteForceAttempt(String email) {
    int failedAttempts = auditLogRepository.countFailedLoginAttemptsInMinutes(
        email, FAILED_LOGIN_WINDOW_MINUTES);
    boolean isBruteForce = failedAttempts >= FAILED_LOGIN_THRESHOLD;
    
    if (isBruteForce && emailService != null) {
      logger.warn("Brute force attempt detected for email: {}", email);
      // Get the IP address from the most recent failed attempt
      List<AuditLogEntity> failedLogins = getFailedLoginAttempts(email, FAILED_LOGIN_WINDOW_MINUTES);
      if (!failedLogins.isEmpty()) {
        String ipAddress = failedLogins.get(0).getIpAddress();
        // Send email alert
        emailService.sendBruteForceAlert(email, ipAddress, failedAttempts);
      }
    }
    
    return isBruteForce;
  }

  /**
   * Verifica brute force por IP
   */
  public boolean isBruteForceByIp(String ipAddress) {
    int failedAttempts = auditLogRepository.countFailedLoginAttemptsByIpInMinutes(
        ipAddress, FAILED_LOGIN_WINDOW_MINUTES);
    boolean isBruteForce = failedAttempts >= FAILED_LOGIN_THRESHOLD;
    
    if (isBruteForce && emailService != null) {
      logger.warn("Brute force attempt detected for IP: {}", ipAddress);
      // Send email alert
      emailService.sendAnomalousIpAlert(ipAddress, failedAttempts, failedAttempts);
    }
    
    return isBruteForce;
  }

  /**
   * Obtém tentativas de login falhadas recentes
   */
  public List<AuditLogEntity> getFailedLoginAttempts(String email, int minutesAgo) {
    LocalDateTime since = LocalDateTime.now().minusMinutes(minutesAgo);
    return auditLogRepository.findFailedLoginAttempts(email, since);
  }

  /**
   * Busca todos os logs de auditoria
   */
  public Page<AuditLogEntity> getAllAuditLogs(Pageable pageable) {
    return auditLogRepository.findAll(pageable);
  }

  /**
   * Busca logs de auditoria por usuário
   */
  public Page<AuditLogEntity> getUserAuditLogs(String userId, Pageable pageable) {
    return auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
  }

  /**
   * Busca logs de auditoria por email
   */
  public Page<AuditLogEntity> getEmailAuditLogs(String email, Pageable pageable) {
    return auditLogRepository.findByEmailOrderByCreatedAtDesc(email, pageable);
  }

  /**
   * Busca logs de auditoria por tipo de evento
   */
  public Page<AuditLogEntity> getEventTypeLogs(String eventType, Pageable pageable) {
    return auditLogRepository.findByEventTypeOrderByCreatedAtDesc(eventType, pageable);
  }

  /**
   * Busca logs de auditoria por IP
   */
  public Page<AuditLogEntity> getIpAddressLogs(String ipAddress, Pageable pageable) {
    return auditLogRepository.findByIpAddressOrderByCreatedAtDesc(ipAddress, pageable);
  }

  /**
   * Busca logs em período específico
   */
  public Page<AuditLogEntity> getLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate,
                                                  Pageable pageable) {
    return auditLogRepository.findByDateRange(startDate, endDate, pageable);
  }

  /**
   * Busca atividades suspeitas recentes
   */
  public Page<AuditLogEntity> getSuspiciousActivity(int hoursAgo, Pageable pageable) {
    LocalDateTime since = LocalDateTime.now().minusHours(hoursAgo);
    return auditLogRepository.findSuspiciousEvents(since, pageable);
  }

  /**
   * Obtém histórico de sessões de um usuário
   */
  public Page<AuditLogEntity> getUserSessions(String userId, Pageable pageable) {
    return auditLogRepository.findUserSessions(userId, pageable);
  }

  /**
   * Obtém atividade anômala (IPs com múltiplas falhas)
   */
  public List<AuditLogEntity> getAnomalousActivity() {
    return auditLogRepository.findAnomalousActivity();
  }

  /**
   * Limpeza agendada de logs antigos (mais de 90 dias)
   * Executa diariamente às 2 da manhã
   */
  @Scheduled(cron = "0 0 2 * * *")
  @Transactional
  public void cleanupOldLogs() {
    LocalDateTime cutoffDate = LocalDateTime.now().minusDays(LOG_RETENTION_DAYS);
    auditLogRepository.deleteOldLogs(cutoffDate);
    logger.info("Audit log cleanup completed - removed logs older than {}", cutoffDate);
  }

  /**
   * Envia relatório de segurança diário
   * Executa diariamente às 8 da manhã
   */
  @Scheduled(cron = "0 0 8 * * *")
  @Transactional
  public void sendDailySecurityReport() {
    try {
      LocalDateTime startOfDay = LocalDateTime.now().minusHours(24);
      
      // Get statistics from yesterday
      long totalEvents = auditLogRepository.count();
      long successfulLogins = auditLogRepository.countByEventTypeAndCreatedAtAfter(
          AuditEventType.LOGIN_SUCCESS.getCode(), startOfDay);
      long failedLogins = auditLogRepository.countByEventTypeAndCreatedAtAfter(
          AuditEventType.LOGIN_FAILURE.getCode(), startOfDay);
      
      java.util.Map<String, Object> reportData = new java.util.HashMap<>();
      reportData.put("totalEvents", totalEvents);
      reportData.put("successfulLogins", successfulLogins);
      reportData.put("failedLogins", failedLogins);
      reportData.put("uniqueIps", "0"); // Can be calculated from logs
      
      if (emailService != null) {
        emailService.sendSecurityReport(reportData);
        logger.info("Daily security report sent");
      }
    } catch (Exception e) {
      logger.error("Error sending daily security report: {}", e.getMessage(), e);
    }
  }

  /**
   * Obter um log de auditoria por ID
   */
  public AuditLogEntity getAuditLog(String id) {
    return auditLogRepository.findById(id).orElse(null);
  }

  /**
   * Contar logs por tipo de evento
   */
  public long countByEventType(String eventType) {
    return auditLogRepository.countByEventType(eventType);
  }

  /**
   * Relatório de segurança: atividades suspeitas nas últimas 24 horas
   */
  public SecurityReport generateSecurityReport() {
    LocalDateTime last24h = LocalDateTime.now().minusHours(24);
    Page<AuditLogEntity> suspiciousEvents = auditLogRepository.findSuspiciousEvents(
        last24h, Pageable.unpaged());

    List<AuditLogEntity> anomalousActivity = auditLogRepository.findAnomalousActivity();

    return new SecurityReport(
        suspiciousEvents.getTotalElements(),
        anomalousActivity.size(),
        suspiciousEvents.getContent(),
        anomalousActivity
    );
  }

  /**
   * Classe para relatório de segurança
   */
  public static class SecurityReport {
    public final long suspiciousEventCount;
    public final int anomalousIpCount;
    public final List<AuditLogEntity> suspiciousEvents;
    public final List<AuditLogEntity> anomalousActivities;

    public SecurityReport(long suspiciousEventCount, int anomalousIpCount,
                         List<AuditLogEntity> suspiciousEvents,
                         List<AuditLogEntity> anomalousActivities) {
      this.suspiciousEventCount = suspiciousEventCount;
      this.anomalousIpCount = anomalousIpCount;
      this.suspiciousEvents = suspiciousEvents;
      this.anomalousActivities = anomalousActivities;
    }

    @Override
    public String toString() {
      return "SecurityReport{" +
          "suspiciousEventCount=" + suspiciousEventCount +
          ", anomalousIpCount=" + anomalousIpCount +
          ", suspiciousEventsCount=" + suspiciousEvents.size() +
          '}';
    }
  }
}
