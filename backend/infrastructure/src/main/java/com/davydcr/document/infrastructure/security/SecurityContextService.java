package com.davydcr.document.infrastructure.security;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utilitário para extrair informações do usuário autenticado.
 * Extrai o userId do header ou session.
 */
@Component
public class SecurityContextService {

    private static final String USER_ID_HEADER = "X-User-ID";
    private static final String AUTH_HEADER = "Authorization";

    /**
     * Extrai o userId do request.
     * Procura em: header X-User-ID → Authorization header → session
     * 
     * @return userId do usuário autenticado ou null se não encontrado
     */
    public String getCurrentUserId() {
        try {
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            
            if (attributes == null) {
                return null;
            }

            HttpServletRequest request = attributes.getRequest();

            // 1. Tentar obter do header X-User-ID
            String userIdFromHeader = request.getHeader(USER_ID_HEADER);
            if (userIdFromHeader != null && !userIdFromHeader.isBlank()) {
                return userIdFromHeader;
            }

            // 2. Tentar obter do Authorization header (Bearer token)
            String authHeader = request.getHeader(AUTH_HEADER);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                // Em um caso real, você decodificaria o JWT aqui
                // Por enquanto, retornar o token como ID
                return token;
            }

            // 3. Tentar obter da session (getAttribute)
            Object userIdFromSession = request.getSession(false) != null ? 
                request.getSession(false).getAttribute("userId") : null;
            
            if (userIdFromSession != null) {
                return userIdFromSession.toString();
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extrai o userId do context, lançando exceção se não autenticado.
     * 
     * @return userId do usuário autenticado
     * @throws IllegalStateException se não autenticado
     */
    public String getCurrentUserIdOrThrow() {
        String userId = getCurrentUserId();
        
        if (userId == null || userId.isBlank()) {
            throw new IllegalStateException("User is not authenticated");
        }
        
        return userId;
    }

    /**
     * Verifica se o usuário atual é dono do recurso.
     * 
     * @param resourceOwnerId ID do proprietário do recurso
     * @return true se o usuário atual é proprietário
     */
    public boolean isOwner(String resourceOwnerId) {
        String currentUserId = getCurrentUserId();
        return currentUserId != null && currentUserId.equals(resourceOwnerId);
    }

    /**
     * Verifica se o usuário está autenticado.
     * 
     * @return true se autenticado
     */
    public boolean isAuthenticated() {
        return getCurrentUserId() != null;
    }
}

