package com.davydcr.document.infrastructure.repository;

import com.davydcr.document.infrastructure.persistence.entity.AuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository para AuditLogEntity.
 * 
 * Fornece queries para auditoria, análise e alertas de segurança.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, String> {

  /**
   * Buscar logs por usuário
   */
  Page<AuditLogEntity> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

  /**
   * Buscar logs por email
   */
  Page<AuditLogEntity> findByEmailOrderByCreatedAtDesc(String email, Pageable pageable);

  /**
   * Buscar logs por tipo de evento
   */
  Page<AuditLogEntity> findByEventTypeOrderByCreatedAtDesc(String eventType, Pageable pageable);

  /**
   * Buscar logs por IP
   */
  Page<AuditLogEntity> findByIpAddressOrderByCreatedAtDesc(String ipAddress, Pageable pageable);

  /**
   * Buscar logs dentro de um período
   */
  @Query("SELECT al FROM AuditLogEntity al " +
      "WHERE al.createdAt BETWEEN :startDate AND :endDate " +
      "ORDER BY al.createdAt DESC")
  Page<AuditLogEntity> findByDateRange(
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate,
      Pageable pageable);

  /**
   * Buscar logs de login falhados de um usuário
   */
  @Query("SELECT al FROM AuditLogEntity al " +
      "WHERE al.email = :email " +
      "AND al.eventType = 'LOGIN_FAILURE' " +
      "AND al.createdAt > :since " +
      "ORDER BY al.createdAt DESC")
  List<AuditLogEntity> findFailedLoginAttempts(
      @Param("email") String email,
      @Param("since") LocalDateTime since);

  /**
   * Contar tentativas de login falhadas por email nos últimos minutos
   */
  @Query(value = "SELECT COUNT(*) FROM audit_logs " +
      "WHERE email = :email " +
      "AND event_type = 'LOGIN_FAILURE' " +
      "AND created_at > DATEADD('MINUTE', -:minutes, NOW())", 
      nativeQuery = true)
  int countFailedLoginAttemptsInMinutes(
      @Param("email") String email,
      @Param("minutes") int minutes);

  /**
   * Contar tentativas de login falhadas por IP nos últimos minutos (brute force detection)
   */
  @Query(value = "SELECT COUNT(*) FROM audit_logs " +
      "WHERE ip_address = :ipAddress " +
      "AND event_type = 'LOGIN_FAILURE' " +
      "AND created_at > DATEADD('MINUTE', -:minutes, NOW())", 
      nativeQuery = true)
  int countFailedLoginAttemptsByIpInMinutes(
      @Param("ipAddress") String ipAddress,
      @Param("minutes") int minutes);

  /**
   * Buscar eventos suspeitos (failuras, acessos não autorizados)
   */
  @Query("SELECT al FROM AuditLogEntity al " +
      "WHERE (al.eventType LIKE '%FAILURE%' " +
      "OR al.eventType LIKE '%UNAUTHORIZED%' " +
      "OR al.eventType LIKE '%FORBIDDEN%') " +
      "AND al.createdAt > :since " +
      "ORDER BY al.createdAt DESC")
  Page<AuditLogEntity> findSuspiciousEvents(
      @Param("since") LocalDateTime since,
      Pageable pageable);

  /**
   * Buscar eventos de um usuário dentro de um período
   */
  @Query("SELECT al FROM AuditLogEntity al " +
      "WHERE al.userId = :userId " +
      "AND al.createdAt BETWEEN :startDate AND :endDate " +
      "ORDER BY al.createdAt DESC")
  List<AuditLogEntity> findUserActivityInPeriod(
      @Param("userId") String userId,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate);

  /**
   * Buscar todos os logins de um usuário
   */
  @Query("SELECT al FROM AuditLogEntity al " +
      "WHERE al.userId = :userId " +
      "AND al.eventType IN ('LOGIN_SUCCESS', 'LOGOUT') " +
      "ORDER BY al.createdAt DESC")
  Page<AuditLogEntity> findUserSessions(
      @Param("userId") String userId,
      Pageable pageable);

  /**
   * Buscar atividades anormais no último dia (múltiplas falhas do mesmo IP)
   */
  @Query(value = "SELECT * FROM audit_logs al " +
      "WHERE al.ip_address IN (" +
      "  SELECT ip_address FROM audit_logs " +
      "  WHERE event_type = 'LOGIN_FAILURE' " +
      "  AND created_at > DATEADD('DAY', -1, NOW()) " +
      "  GROUP BY ip_address " +
      "  HAVING COUNT(*) > 5" +
      ") " +
      "AND al.created_at > DATEADD('DAY', -1, NOW()) " +
      "ORDER BY al.created_at DESC",
      nativeQuery = true)
  List<AuditLogEntity> findAnomalousActivity();

  /**
   * Limpar logs antigos (mais de N dias)
   */
  @Modifying
  @Transactional
  @Query("DELETE FROM AuditLogEntity al WHERE al.createdAt < :before")
  void deleteOldLogs(@Param("before") LocalDateTime before);

  /**
   * Contar logs por tipo de evento
   */
  @Query("SELECT COUNT(al) FROM AuditLogEntity al WHERE al.eventType = :eventType")
  long countByEventType(@Param("eventType") String eventType);

  /**
   * Contar logs por tipo de evento e data
   */
  @Query("SELECT COUNT(al) FROM AuditLogEntity al WHERE al.eventType = :eventType AND al.createdAt >= :createdAtAfter")
  long countByEventTypeAndCreatedAtAfter(@Param("eventType") String eventType, @Param("createdAtAfter") LocalDateTime createdAtAfter);
}
