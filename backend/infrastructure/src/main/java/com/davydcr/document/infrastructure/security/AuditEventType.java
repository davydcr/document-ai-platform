package com.davydcr.document.infrastructure.security;

/**
 * Tipos de eventos de auditoria.
 * 
 * Categorias:
 * - LOGIN_*: Autenticação
 * - LOGOUT_*: Encerramento de sessão
 * - REFRESH_*: Renovação de tokens
 * - ACCESS_*: Controle de acesso
 * - CHANGE_*: Mudanças de credenciais
 * - ADMIN_*: Operações administrativas
 */
public enum AuditEventType {

  // ===== AUTENTICAÇÃO =====
  /**
   * Login bem-sucedido
   */
  LOGIN_SUCCESS("LOGIN_SUCCESS", "Usuário fez login com sucesso"),

  /**
   * Tentativa de login falhada (credenciais inválidas)
   */
  LOGIN_FAILURE("LOGIN_FAILURE", "Falha na autenticação - credenciais inválidas"),

  /**
   * Login bloqueado por rate limiting
   */
  LOGIN_RATE_LIMIT_EXCEEDED("LOGIN_RATE_LIMIT_EXCEEDED", "Tentativas de login excedidas - bloqueado por rate limit"),

  /**
   * Conta bloqueada/suspensa
   */
  LOGIN_ACCOUNT_LOCKED("LOGIN_ACCOUNT_LOCKED", "Tentativa de login em conta bloqueada"),

  // ===== LOGOUT =====
  /**
   * Logout bem-sucedido
   */
  LOGOUT_SUCCESS("LOGOUT_SUCCESS", "Usuário fez logout"),

  /**
   * Logout forçado (sessão expirada ou revogada)
   */
  LOGOUT_FORCED("LOGOUT_FORCED", "Sessão encerrada pelo sistema"),

  // ===== TOKEN =====
  /**
   * Refresh de token bem-sucedido
   */
  TOKEN_REFRESH_SUCCESS("TOKEN_REFRESH_SUCCESS", "Token renovado com sucesso"),

  /**
   * Refresh token inválido/expirado
   */
  TOKEN_REFRESH_FAILURE("TOKEN_REFRESH_FAILURE", "Falha na renovação - token inválido/expirado"),

  /**
   * Token revogado
   */
  TOKEN_REVOKED("TOKEN_REVOKED", "Refresh token revogado"),

  // ===== ACESSO =====
  /**
   * Acesso autorizado
   */
  ACCESS_GRANTED("ACCESS_GRANTED", "Acesso autorizado ao recurso"),

  /**
   * Acesso negado (não autenticado)
   */
  ACCESS_UNAUTHORIZED("ACCESS_UNAUTHORIZED", "Acesso negado - não autenticado"),

  /**
   * Acesso negado (permissões insuficientes)
   */
  ACCESS_FORBIDDEN("ACCESS_FORBIDDEN", "Acesso negado - permissões insuficientes"),

  /**
   * Acesso a recurso não encontrado
   */
  ACCESS_NOT_FOUND("ACCESS_NOT_FOUND", "Recurso não encontrado"),

  // ===== MUDANÇAS DE CREDENCIAIS =====
  /**
   * Senha alterada
   */
  PASSWORD_CHANGED("PASSWORD_CHANGED", "Senha alterada com sucesso"),

  /**
   * Reset de senha solicitado
   */
  PASSWORD_RESET_REQUESTED("PASSWORD_RESET_REQUESTED", "Reset de senha solicitado"),

  /**
   * Reset de senha completado
   */
  PASSWORD_RESET_COMPLETED("PASSWORD_RESET_COMPLETED", "Reset de senha completado"),

  /**
   * Email alterado
   */
  EMAIL_CHANGED("EMAIL_CHANGED", "Email alterado com sucesso"),

  /**
   * 2FA ativado
   */
  TWO_FACTOR_ENABLED("TWO_FACTOR_ENABLED", "Autenticação de dois fatores ativada"),

  /**
   * 2FA desativado
   */
  TWO_FACTOR_DISABLED("TWO_FACTOR_DISABLED", "Autenticação de dois fatores desativada"),

  // ===== OPERAÇÕES ADMINISTRATIVAS =====
  /**
   * Usuário criado
   */
  USER_CREATED("USER_CREATED", "Novo usuário criado"),

  /**
   * Usuário atualizado
   */
  USER_UPDATED("USER_UPDATED", "Dados de usuário atualizados"),

  /**
   * Usuário deletado
   */
  USER_DELETED("USER_DELETED", "Usuário deletado"),

  /**
   * Role atribuída a usuário
   */
  ROLE_ASSIGNED("ROLE_ASSIGNED", "Role atribuída ao usuário"),

  /**
   * Role removida de usuário
   */
  ROLE_REVOKED("ROLE_REVOKED", "Role removida do usuário"),

  /**
   * Usuário bloqueado
   */
  USER_LOCKED("USER_LOCKED", "Usuário bloqueado"),

  /**
   * Usuário desbloqueado
   */
  USER_UNLOCKED("USER_UNLOCKED", "Usuário desbloqueado"),

  // ===== EVENTOS DE SISTEMA =====
  /**
   * Erro de sistema
   */
  SYSTEM_ERROR("SYSTEM_ERROR", "Erro de sistema"),

  /**
   * Operação suspeita detectada
   */
  SUSPICIOUS_ACTIVITY("SUSPICIOUS_ACTIVITY", "Atividade suspeita detectada"),

  /**
   * Evento genérico
   */
  GENERIC("GENERIC", "Evento genérico");

  private final String code;
  private final String description;

  AuditEventType(String code, String description) {
    this.code = code;
    this.description = description;
  }

  public String getCode() {
    return code;
  }

  public String getDescription() {
    return description;
  }

  /**
   * Verifica se é um evento de sucesso
   */
  public boolean isSuccess() {
    return code.contains("SUCCESS");
  }

  /**
   * Verifica se é um evento de falha
   */
  public boolean isFailure() {
    return code.contains("FAILURE") || code.contains("DENIED") || 
           code.contains("INVALID") || code.contains("EXPIRED") ||
           code.contains("UNAUTHORIZED") || code.contains("FORBIDDEN") ||
           code.contains("LOCKED") || code.contains("ERROR");
  }

  /**
   * Verifica se é um evento suspeito
   */
  public boolean isSuspicious() {
    return code.contains("FAILURE") || code.contains("UNAUTHORIZED") ||
           code.contains("FORBIDDEN") || code.contains("SUSPICIOUS");
  }

  @Override
  public String toString() {
    return code;
  }
}
