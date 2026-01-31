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
 * Também suporta ThreadLocal para processamento assíncrono.
 */
@Service
public class SecurityContextService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityContextService.class);
    
    // ThreadLocal para propagar userId para threads assíncronas
    private static final ThreadLocal<String> asyncUserIdHolder = new ThreadLocal<>();

    /**
     * Define o userId para a thread atual (usado em processamento assíncrono).
     */
    public static void setAsyncUserId(String userId) {
        asyncUserIdHolder.set(userId);
    }

    /**
     * Limpa o userId da thread atual.
     */
    public static void clearAsyncUserId() {
        asyncUserIdHolder.remove();
    }

    /**
     * Obtém o ID do usuário atual.
     * Ordem de prioridade:
     * 1. ThreadLocal (para processamento assíncrono)
     * 2. Request attribute (setado pelo JwtAuthenticationFilter)
     * 3. SecurityContextHolder (fallback)
     */
    public String getCurrentUserId() {
        try {
            // 1. Primeiro, verificar ThreadLocal (para processamento assíncrono)
            String asyncUserId = asyncUserIdHolder.get();
            if (asyncUserId != null) {
                logger.debug("getCurrentUserId() from ThreadLocal: {}", asyncUserId);
                return asyncUserId;
            }
            
            // 2. Tentar obter do request attribute (setado pelo JwtAuthenticationFilter)
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                Object userId = request.getAttribute("userId");
                logger.debug("getCurrentUserId() request attribute userId={}", userId);
                if (userId != null) {
                    return userId.toString();
                }
            }
            
            // 3. Fallback: tentar do SecurityContextHolder
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null) {
                String principal = auth.getPrincipal().toString();
                if (!"anonymousUser".equals(principal)) {
                    logger.debug("getCurrentUserId() from SecurityContext: {}", principal);
                    return principal;
                }
            }
            
            logger.debug("getCurrentUserId() returning null - no user context available");
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

