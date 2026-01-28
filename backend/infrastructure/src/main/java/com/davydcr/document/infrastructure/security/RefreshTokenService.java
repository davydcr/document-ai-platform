package com.davydcr.document.infrastructure.security;

import com.davydcr.document.infrastructure.persistence.entity.RefreshTokenEntity;
import com.davydcr.document.infrastructure.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço para gerenciar Refresh Tokens
 */
@Service
public class RefreshTokenService {
    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtProvider jwtProvider) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProvider = jwtProvider;
    }

    /**
     * Cria um novo refresh token para um usuário
     */
    @Transactional
    public RefreshTokenEntity createRefreshToken(String userId) {
        logger.info("Criando refresh token para usuário: {}", userId);

        RefreshTokenEntity token = new RefreshTokenEntity();
        token.setId(UUID.randomUUID().toString());
        token.setUserId(userId);
        token.setToken(jwtProvider.generateRefreshToken());
        token.setExpiresAt(jwtProvider.getRefreshTokenExpiryDate());
        token.setRevoked(false);
        token.setCreatedAt(LocalDateTime.now());
        token.setUpdatedAt(LocalDateTime.now());

        return refreshTokenRepository.save(token);
    }

    /**
     * Valida um refresh token
     */
    public Optional<RefreshTokenEntity> validateRefreshToken(String token) {
        logger.debug("Validando refresh token: {}", token);

        Optional<RefreshTokenEntity> refreshToken = refreshTokenRepository.findByToken(token);

        if (refreshToken.isPresent()) {
            RefreshTokenEntity rt = refreshToken.get();
            
            // Verificar se está revogado
            if (rt.getRevoked()) {
                logger.warn("Refresh token revogado: {}", token);
                return Optional.empty();
            }

            // Verificar se expirou
            if (rt.isExpired()) {
                logger.warn("Refresh token expirado: {}", token);
                return Optional.empty();
            }

            logger.debug("Refresh token válido para usuário: {}", rt.getUserId());
            return Optional.of(rt);
        }

        logger.warn("Refresh token não encontrado: {}", token);
        return Optional.empty();
    }

    /**
     * Revoga um refresh token
     */
    @Transactional
    public void revokeRefreshToken(String token) {
        logger.info("Revogando refresh token: {}", token);

        Optional<RefreshTokenEntity> refreshToken = refreshTokenRepository.findByToken(token);
        if (refreshToken.isPresent()) {
            refreshTokenRepository.revokeById(refreshToken.get().getId());
        }
    }

    /**
     * Revoga todos os refresh tokens de um usuário (logout em todos os devices)
     */
    @Transactional
    public void revokeAllUserTokens(String userId) {
        logger.info("Revogando todos os refresh tokens do usuário: {}", userId);
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    /**
     * Limpeza periódica de tokens expirados (executada diariamente)
     */
    @Scheduled(fixedDelay = 86400000) // Executar a cada 24 horas
    @Transactional
    public void cleanupExpiredTokens() {
        logger.info("Iniciando limpeza de refresh tokens expirados");

        int deleted = refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        logger.info("Removidos {} refresh tokens expirados", deleted);
    }

    /**
     * Conta quantos refresh tokens válidos um usuário possui
     */
    public long countValidTokensForUser(String userId) {
        return refreshTokenRepository.countValidTokensByUserId(userId);
    }
}
