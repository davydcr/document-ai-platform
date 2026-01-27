package com.davydcr.document.infrastructure.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * JWT Filter - Extrai e valida JWT tokens nas requisições
 * Implementação minimalista sem Spring Security complexo
 */
public class JwtFilter implements Filter {

    private final JwtProvider jwtProvider;

    public JwtFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();

        // Endpoints públicos não precisam de autenticação
        if (isPublicEndpoint(path)) {
            chain.doFilter(request, response);
            return;
        }

        // Verificar X-User-ID header primeiro (mais simples, para testes e APIs simples)
        String userId = httpRequest.getHeader("X-User-ID");
        if (userId != null && !userId.isBlank()) {
            // X-User-ID fornecido - permitir requisição
            httpRequest.setAttribute("userId", userId);
            chain.doFilter(request, response);
            return;
        }

        // Tentar JWT token (mais complexo, para produção com Spring Security)
        String token = extractToken(httpRequest);

        if (token != null && jwtProvider.isTokenValid(token)) {
            // Token válido - adiciona informações à requisição
            String username = jwtProvider.getUsernameFromToken(token);
            httpRequest.setAttribute("username", username);
            httpRequest.setAttribute("roles", jwtProvider.getRolesFromToken(token));
            chain.doFilter(request, response);
        } else {
            // Nenhuma forma de autenticação válida
            httpResponse.setStatus(401);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"error\": \"Unauthorized\"}");
        }
    }

    private boolean isPublicEndpoint(String path) {
        // Path may include context like: /api/auth/health or just /auth/health
        // Remove /api prefix if present for cleaner matching
        if (path.startsWith("/api")) {
            path = path.substring(4);
        }
        
        return path.startsWith("/auth") ||
               path.startsWith("/actuator") ||
               path.contains("swagger") ||
               path.contains("api-docs");
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
