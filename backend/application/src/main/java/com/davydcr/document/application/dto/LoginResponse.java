package com.davydcr.document.application.dto;

import java.util.Set;

/**
 * Resposta de autenticação com JWT token
 */
public record LoginResponse(
        String token,
        String type,
        String username,
        Set<String> roles,
        Long expiresIn
) {
    public static LoginResponse bearer(String token, String username, Set<String> roles, Long expiresInSeconds) {
        return new LoginResponse(token, "Bearer", username, roles, expiresInSeconds);
    }
}
