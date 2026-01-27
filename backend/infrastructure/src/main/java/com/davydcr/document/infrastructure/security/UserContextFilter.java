package com.davydcr.document.infrastructure.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filter que extrai o userId do header X-User-ID e o armazena em ThreadLocal.
 * Permite acesso fácil ao userId em qualquer ponto da aplicação.
 */
@Component
public class UserContextFilter implements Filter {

    private static final String USER_ID_HEADER = "X-User-ID";
    private static final ThreadLocal<String> userIdThreadLocal = new ThreadLocal<>();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Nada a fazer na inicialização
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            if (request instanceof HttpServletRequest) {
                HttpServletRequest httpRequest = (HttpServletRequest) request;
                String userId = httpRequest.getHeader(USER_ID_HEADER);
                System.out.println("[UserContextFilter] Recebido header X-User-ID: " + userId);
                if (userId != null && !userId.isBlank()) {
                    userIdThreadLocal.set(userId);
                    System.out.println("[UserContextFilter] UserId definido em ThreadLocal: " + userId);
                }
            }
            chain.doFilter(request, response);
        } finally {
            // Limpar ThreadLocal para evitar memory leaks
            userIdThreadLocal.remove();
        }
    }

    @Override
    public void destroy() {
        // Nada a fazer na destruição
    }

    /**
     * Obtém o userId do ThreadLocal.
     * 
     * @return userId ou null se não definido
     */
    public static String getUserId() {
        return userIdThreadLocal.get();
    }

    /**
     * Define o userId no ThreadLocal (principalmente para testes).
     * 
     * @param userId ID do usuário
     */
    public static void setUserId(String userId) {
        if (userId == null) {
            userIdThreadLocal.remove();
        } else {
            userIdThreadLocal.set(userId);
        }
    }

    /**
     * Limpa o ThreadLocal.
     */
    public static void clear() {
        userIdThreadLocal.remove();
    }
}
