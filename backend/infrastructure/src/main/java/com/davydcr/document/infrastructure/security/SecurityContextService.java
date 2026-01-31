package com.davydcr.document.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Serviço para acesso ao contexto de segurança.
 * Lê o userId do atributo da requisição (setado pelo JwtAuthenticationFilter).
 */
@Service
public class SecurityContextService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityContextService.class);

    /**
     * Obtém o ID do usuário atual.
     * Primeiro tenta ler do request attribute (mais confiável),
     * depois do SecurityContextHolder como fallback.
     */
    public String getCurrentUserId() {
        try {
            // Primeiro, tentar obter do request attribute (setado pelo JwtAuthenticationFilter)
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                Object userId = request.getAttribute("userId");
                logger.info("getCurrentUserId() request attribute userId={}", userId);
                if (userId != null) {
                    return userId.toString();
                }
            } else {
                logger.info("getCurrentUserId() ServletRequestAttributes is null");
            }
            
            // Fallback: tentar do SecurityContextHolder
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            logger.info("getCurrentUserId() SecurityContextHolder auth={}", auth);
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null) {
                String principal = auth.getPrincipal().toString();
                logger.info("getCurrentUserId() principal={}", principal);
                if (!"anonymousUser".equals(principal)) {
                    return principal;
                }
            }
            
            logger.info("getCurrentUserId() returning null");
            return null;
        } catch (Exception e) {
            logger.error("getCurrentUserId() error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Verifica se existe um usuário autenticado no contexto.
     */
    public boolean isAuthenticated() {
        String userId = getCurrentUserId();
        return userId != null && !userId.isEmpty();
    }

    /**
     * Verifica se o usuário atual é o proprietário do recurso.
     * @param ownerId ID do proprietário do recurso
     * @return true se o usuário atual é o proprietário
     */
    public boolean isOwner(String ownerId) {
        String currentUserId = getCurrentUserId();
        return currentUserId != null && currentUserId.equals(ownerId);
    }

    /**
     * Verifica se o usuário atual tem acesso ao recurso (é o proprietário).
     * @param ownerId ID do proprietário do recurso
     * @return true se o usuário tem acesso
     */
    public boolean hasAccess(String ownerId) {
        return isOwner(ownerId);
    }
}

