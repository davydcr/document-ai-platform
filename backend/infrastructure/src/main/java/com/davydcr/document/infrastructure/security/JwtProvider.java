package com.davydcr.document.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * JWT Provider - Gera e valida tokens JWT simples
 * Suporta access tokens e refresh tokens
 */
@Component
public class JwtProvider {

    private final SecretKey secretKey;
    private final long expirationMs;
    private final long refreshTokenExpirationMs;

    public JwtProvider(
            @Value("${app.jwt.secret:your-secret-key-change-this-in-production-at-least-32-bytes-long}") String secret,
            @Value("${app.jwt.expiration:86400000}") long expirationMs,
            @Value("${app.jwt.refresh.expiration:2592000000}") long refreshTokenExpirationMs) {
        
        this.expirationMs = expirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Gera um JWT token de acesso (access token)
     */
    public String generateToken(String userId, String username, Set<String> roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId.toString())
                .claim("roles", roles)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Valida um token JWT
     */
    public boolean isTokenValid(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extrai username do token
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.getSubject();
    }

    /**
     * Extrai userId do token (novo método)
     */
    public String extractUserId(String token) {
        Claims claims = getClaims(token);
        Object userId = claims.get("userId");
        return userId != null ? userId.toString() : claims.getSubject();
    }

    /**
     * Valida um token JWT
     */
    public boolean validateToken(String token) {
        return isTokenValid(token);
    }

    /**
     * Extrai roles do token
     */
    @SuppressWarnings("unchecked")
    public Set<String> getRolesFromToken(String token) {
        Claims claims = getClaims(token);
        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof Collection) {
            return new HashSet<>((Collection<String>) rolesObj);
        }
        return Set.of();
    }

    /**
     * Extrai claims do token
     */
    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Gera um refresh token (string aleatória em UUID)
     * Nota: O refresh token é armazenado no banco e validado lá
     */
    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * Calcula data de expiração para refresh token
     */
    public LocalDateTime getRefreshTokenExpiryDate() {
        return LocalDateTime.now().plusSeconds(refreshTokenExpirationMs / 1000);
    }

    /**
     * Retorna tempo de expiração do access token em ms
     */
    public long getAccessTokenExpirationMs() {
        return expirationMs;
    }

    /**
     * Retorna tempo de expiração do refresh token em ms
     */
    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }
}
