package com.davydcr.document.infrastructure.security;

import com.davydcr.document.infrastructure.repository.AuditLogRepository;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener para eventos de autenticação do Spring Security.
 * 
 * Responsabilidades:
 * - Capturar login bem-sucedidos
 * - Capturar falhas de autenticação
 * - Registrar no audit_logs automaticamente
 */
@Component
public class AuthenticationEventListener implements ApplicationListener<ApplicationEvent> {

  private static final Logger logger = LoggerFactory.getLogger(AuthenticationEventListener.class);

  private final AuditLogService auditLogService;
  private final AuditLogRepository auditLogRepository;

  public AuthenticationEventListener(AuditLogService auditLogService,
                                    AuditLogRepository auditLogRepository) {
    this.auditLogService = auditLogService;
    this.auditLogRepository = auditLogRepository;
  }

  @Override
  public void onApplicationEvent(ApplicationEvent event) {
    if (event instanceof AuthenticationSuccessEvent) {
      handleAuthenticationSuccess((AuthenticationSuccessEvent) event);
    } else if (event instanceof AuthenticationFailureBadCredentialsEvent) {
      handleAuthenticationFailure((AuthenticationFailureBadCredentialsEvent) event);
    }
  }

  /**
   * Trata sucesso na autenticação
   */
  private void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
    try {
      Authentication auth = event.getAuthentication();
      String principal = (String) auth.getPrincipal();
      
      // Obter IP e user-agent da requisição (via SecurityContext ou RequestContextHolder)
      String ipAddress = getClientIpAddress();
      String userAgent = getClientUserAgent();
      
      auditLogService.logLoginSuccess(principal, principal, ipAddress, userAgent, "/api/auth/login");
      logger.info("Authentication success logged for: {}", principal);
    } catch (Exception e) {
      logger.error("Error logging authentication success", e);
    }
  }

  /**
   * Trata falha na autenticação
   */
  private void handleAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
    try {
      Authentication auth = event.getAuthentication();
      String principal = auth.getName();
      String errorMessage = event.getException().getMessage();
      
      // Obter IP e user-agent da requisição
      String ipAddress = getClientIpAddress();
      String userAgent = getClientUserAgent();
      
      auditLogService.logLoginFailure(principal, ipAddress, userAgent, "/api/auth/login", errorMessage);
      logger.info("Authentication failure logged for: {}", principal);
    } catch (Exception e) {
      logger.error("Error logging authentication failure", e);
    }
  }

  /**
   * Obtém o endereço IP do cliente
   */
  private String getClientIpAddress() {
    // Este é um placeholder - em um contexto real, você buscaria do RequestContextHolder
    return "unknown";
  }

  /**
   * Obtém o user-agent do cliente
   */
  private String getClientUserAgent() {
    // Este é um placeholder - em um contexto real, você buscaria do RequestContextHolder
    return "unknown";
  }
}
