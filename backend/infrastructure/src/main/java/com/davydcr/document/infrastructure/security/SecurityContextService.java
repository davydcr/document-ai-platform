package com.davydcr.document.infrastructure.security;

import org.springframework.stereotype.Component;

/**
 * Utilitário para extrair informações do usuário autenticado.
 * Utiliza ThreadLocal configurado pelo UserContextFilter.
 */
@Component
public class SecurityContextService {

    /**
     * Extrai o userId do contexto de thread.
     * O userId é preenchido pelo UserContextFilter do header X-User-ID.
     * 
     * @return userId do usuário autenticado ou null se não encontrado
     */
    public String getCurrentUserId() {
        try {
            return UserContextFilter.getUserId();
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

