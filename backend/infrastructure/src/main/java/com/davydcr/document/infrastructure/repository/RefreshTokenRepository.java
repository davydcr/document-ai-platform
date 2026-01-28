package com.davydcr.document.infrastructure.repository;

import com.davydcr.document.infrastructure.persistence.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositório para gerenciar Refresh Tokens
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, String> {

    /**
     * Busca um refresh token pelo token string
     */
    Optional<RefreshTokenEntity> findByToken(String token);

    /**
     * Busca todos os refresh tokens de um usuário
     */
    List<RefreshTokenEntity> findByUserId(String userId);

    /**
     * Busca refresh tokens válidos (não revogados e não expirados) de um usuário
     */
    @Query("SELECT rt FROM RefreshTokenEntity rt WHERE rt.userId = :userId AND rt.revoked = false AND rt.expiresAt > CURRENT_TIMESTAMP")
    List<RefreshTokenEntity> findValidTokensByUserId(@Param("userId") String userId);

    /**
     * Revoga um refresh token por ID
     */
    @Modifying
    @Transactional
    @Query("UPDATE RefreshTokenEntity rt SET rt.revoked = true, rt.updatedAt = CURRENT_TIMESTAMP WHERE rt.id = :id")
    int revokeById(@Param("id") String id);

    /**
     * Revoga todos os refresh tokens de um usuário
     */
    @Modifying
    @Transactional
    @Query("UPDATE RefreshTokenEntity rt SET rt.revoked = true, rt.updatedAt = CURRENT_TIMESTAMP WHERE rt.userId = :userId")
    int revokeAllByUserId(@Param("userId") String userId);

    /**
     * Remove tokens expirados (limpeza periódica)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshTokenEntity rt WHERE rt.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * Conta refresh tokens válidos para um usuário
     */
    @Query("SELECT COUNT(rt) FROM RefreshTokenEntity rt WHERE rt.userId = :userId AND rt.revoked = false AND rt.expiresAt > CURRENT_TIMESTAMP")
    long countValidTokensByUserId(@Param("userId") String userId);
}
