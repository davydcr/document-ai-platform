package com.davydcr.document.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidade JPA para logs de auditoria.
 * 
 * Rastreia todas as atividades de autenticação e acesso na plataforma:
 * - Logins bem-sucedidos e falhados
 * - Logouts
 * - Refresh de tokens
 * - Tentativas de acesso não autorizados
 * - Mudanças de senha
 * - Acessos a recursos críticos
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_logs_user_id", columnList = "user_id"),
    @Index(name = "idx_audit_logs_email", columnList = "email"),
    @Index(name = "idx_audit_logs_event_type", columnList = "event_type"),
    @Index(name = "idx_audit_logs_created_at", columnList = "created_at DESC"),
    @Index(name = "idx_audit_logs_ip_address", columnList = "ip_address"),
    @Index(name = "idx_audit_logs_user_event_date", columnList = "user_id,event_type,created_at DESC"),
    @Index(name = "idx_audit_logs_ip_event_type", columnList = "ip_address,event_type,created_at DESC")
})
public class AuditLogEntity {

  @Id
  private String id;

  @Column(nullable = false, length = 50)
  private String eventType;

  @Column(name = "user_id", length = 36)
  private String userId;

  @Column(nullable = false, length = 255)
  private String email;

  @Column(name = "ip_address", nullable = false, length = 45)
  private String ipAddress;

  @Column(name = "user_agent", columnDefinition = "TEXT")
  private String userAgent;

  @Column(nullable = false, length = 255)
  private String endpoint;

  @Column(nullable = false, length = 10)
  private String method;

  @Column(name = "status_code")
  private Integer statusCode;

  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  @Column(columnDefinition = "TEXT")
  private String details;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  public AuditLogEntity() {
  }

  private AuditLogEntity(Builder builder) {
    this.id = builder.id;
    this.eventType = builder.eventType;
    this.userId = builder.userId;
    this.email = builder.email;
    this.ipAddress = builder.ipAddress;
    this.userAgent = builder.userAgent;
    this.endpoint = builder.endpoint;
    this.method = builder.method;
    this.statusCode = builder.statusCode;
    this.errorMessage = builder.errorMessage;
    this.details = builder.details;
    this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  // Getters
  public String getId() {
    return id;
  }

  public String getEventType() {
    return eventType;
  }

  public String getUserId() {
    return userId;
  }

  public String getEmail() {
    return email;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public String getMethod() {
    return method;
  }

  public Integer getStatusCode() {
    return statusCode;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public String getDetails() {
    return details;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  /**
   * Verifica se é um evento de falha
   */
  public boolean isFailure() {
    return statusCode != null && statusCode >= 400;
  }

  /**
   * Verifica se é um evento de login
   */
  public boolean isLoginEvent() {
    return eventType.equals("LOGIN_SUCCESS") || eventType.equals("LOGIN_FAILURE");
  }

  /**
   * Verifica se é um evento suspeito
   */
  public boolean isSuspicious() {
    return eventType.contains("FAILURE") || eventType.contains("UNAUTHORIZED") ||
        eventType.contains("FORBIDDEN");
  }

  /**
   * Builder para criação fluida
   */
  public static class Builder {
    private String id;
    private String eventType;
    private String userId;
    private String email;
    private String ipAddress;
    private String userAgent;
    private String endpoint;
    private String method;
    private Integer statusCode;
    private String errorMessage;
    private String details;
    private LocalDateTime createdAt;

    public Builder(String id, String eventType, String email, String ipAddress) {
      this.id = id;
      this.eventType = eventType;
      this.email = email;
      this.ipAddress = ipAddress;
    }

    public Builder userId(String userId) {
      this.userId = userId;
      return this;
    }

    public Builder userAgent(String userAgent) {
      this.userAgent = userAgent;
      return this;
    }

    public Builder endpoint(String endpoint) {
      this.endpoint = endpoint;
      return this;
    }

    public Builder method(String method) {
      this.method = method;
      return this;
    }

    public Builder statusCode(Integer statusCode) {
      this.statusCode = statusCode;
      return this;
    }

    public Builder errorMessage(String errorMessage) {
      this.errorMessage = errorMessage;
      return this;
    }

    public Builder details(String details) {
      this.details = details;
      return this;
    }

    public Builder createdAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public AuditLogEntity build() {
      return new AuditLogEntity(this);
    }
  }

  @Override
  public String toString() {
    return "AuditLogEntity{" +
        "id='" + id + '\'' +
        ", eventType='" + eventType + '\'' +
        ", userId='" + userId + '\'' +
        ", email='" + email + '\'' +
        ", ipAddress='" + ipAddress + '\'' +
        ", endpoint='" + endpoint + '\'' +
        ", method='" + method + '\'' +
        ", statusCode=" + statusCode +
        ", createdAt=" + createdAt +
        '}';
  }
}
