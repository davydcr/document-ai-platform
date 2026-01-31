package com.davydcr.document.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Filtro de autenticação JWT
 * Extrai o token JWT do header Authorization e o valida
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtProvider jwtProvider;

    public JwtAuthenticationFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // Tentar extrair token JWT do header Authorization
            String authHeader = request.getHeader("Authorization");
            String token = null;

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }

            // Se houver token válido, autenticar
            if (token != null && jwtProvider.isTokenValid(token)) {
                String userId = jwtProvider.extractUserId(token);
                
                // Criar autenticação e setar no contexto de segurança
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(userId, null, new ArrayList<>());
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                // IMPORTANTE: Também setar no UserContextFilter ThreadLocal para compatibilidade com SecurityContextService
                UserContextFilter.setUserId(userId);
                
                // Também setar no atributo da requisição
                request.setAttribute("userId", userId);
                
                // Log com INFO para garantir visibilidade
                logger.info("JWT token validado para usuário: {} - setAttribute userId={}", userId, userId);
            } else if (token == null) {
                logger.debug("Nenhum token JWT encontrado no header Authorization");
            } else {
                logger.debug("Token JWT inválido ou expirado");
            }
        } catch (Exception e) {
            logger.debug("Erro ao validar JWT token: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
