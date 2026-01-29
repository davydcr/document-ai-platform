package com.davydcr.document.infrastructure.controller;

import com.davydcr.document.application.dto.LoginRequest;
import com.davydcr.document.application.dto.LoginResponse;
import com.davydcr.document.infrastructure.persistence.entity.RefreshTokenEntity;
import com.davydcr.document.infrastructure.persistence.entity.UserAccountEntity;
import com.davydcr.document.infrastructure.repository.UserRepository;
import com.davydcr.document.infrastructure.security.AuditLogService;
import com.davydcr.document.infrastructure.security.JwtProvider;
import com.davydcr.document.infrastructure.security.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Authentication Controller - Endpoint para login com JWT
 * Fornece autenticação, validação de tokens e refresh tokens
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Endpoints de autenticação e autorização")
@CrossOrigin(origins = "*")
public class AuthController {

    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @Value("${app.jwt.expiration:86400000}")
    private long jwtExpirationMs;

    public AuthController(JwtProvider jwtProvider, RefreshTokenService refreshTokenService, 
                         UserRepository userRepository, PasswordEncoder passwordEncoder,
                         AuditLogService auditLogService) {
        this.jwtProvider = jwtProvider;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    /**
     * Login endpoint - retorna JWT token e refresh token
     * Autentica com credenciais do banco de dados
     */
    @PostMapping("/login")
    @Operation(summary = "Login", description = "Faz login com email e senha, retorna JWT token e refresh token")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = getClientUserAgent(httpRequest);
        
        Optional<UserAccountEntity> user = userRepository.findByEmail(request.username());
        
        if (user.isEmpty() || !user.get().getActive()) {
            // Log de falha - credenciais inválidas
            auditLogService.logLoginFailure(request.username(), ipAddress, userAgent, 
                                           "/api/auth/login", "Credenciais inválidas");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciais inválidas"));
        }

        UserAccountEntity userAccount = user.get();
        
        // Validar senha com BCrypt
        if (!passwordEncoder.matches(request.password(), userAccount.getPasswordHash())) {
            // Log de falha - senha incorreta
            auditLogService.logLoginFailure(userAccount.getEmail(), ipAddress, userAgent,
                                           "/api/auth/login", "Senha incorreta");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciais inválidas"));
        }

        // Extrair roles
        Set<String> roles = userAccount.getRoles()
                .stream()
                .map(role -> role.getName())
                .collect(Collectors.toSet());

        // Gerar token JWT
        String token = jwtProvider.generateToken(userAccount.getId(), userAccount.getEmail(), roles);

        // Gerar refresh token e salvar no banco
        RefreshTokenEntity refreshToken = refreshTokenService.createRefreshToken(userAccount.getId());

        // Log de sucesso
        auditLogService.logLoginSuccess(userAccount.getId(), userAccount.getEmail(), 
                                       ipAddress, userAgent, "/api/auth/login");

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("refreshToken", refreshToken.getToken());
        response.put("type", "Bearer");
        response.put("email", userAccount.getEmail());
        response.put("roles", roles);
        response.put("expiresIn", jwtExpirationMs / 1000);

        return ResponseEntity.ok(response);
    }

    /**
     * Refresh token endpoint - renova um access token usando refresh token
     */
    @PostMapping("/refresh")
    @Operation(summary = "Renovar token", description = "Usa refresh token para obter novo access token")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request, 
                                         HttpServletRequest httpRequest) {
        String refreshTokenValue = request.get("refreshToken");
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = getClientUserAgent(httpRequest);

        if (refreshTokenValue == null || refreshTokenValue.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Refresh token é obrigatório"));
        }

        try {
            // Validar refresh token
            Optional<RefreshTokenEntity> validToken = refreshTokenService.validateRefreshToken(refreshTokenValue);

            if (validToken.isEmpty()) {
                // Log de falha - token inválido
                auditLogService.logTokenRefreshFailure("unknown", ipAddress, userAgent, "/api/auth/refresh");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Refresh token inválido ou expirado"));
            }

            RefreshTokenEntity rt = validToken.get();
            
            // Buscar usuário
            Optional<UserAccountEntity> user = userRepository.findByUserId(rt.getUserId());
            if (user.isEmpty() || !user.get().getActive()) {
                // Log de falha - usuário não encontrado
                auditLogService.logTokenRefreshFailure(rt.getUserId(), ipAddress, userAgent, "/api/auth/refresh");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Usuário não encontrado ou inativo"));
            }

            UserAccountEntity userAccount = user.get();

            // Extrair roles
            Set<String> roles = userAccount.getRoles()
                    .stream()
                    .map(role -> role.getName())
                    .collect(Collectors.toSet());

            // Gerar novo access token
            String newAccessToken = jwtProvider.generateToken(userAccount.getId(), userAccount.getEmail(), roles);

            // Log de sucesso
            auditLogService.logTokenRefresh(userAccount.getId(), userAccount.getEmail(), 
                                           ipAddress, userAgent, "/api/auth/refresh");

            Map<String, Object> response = new HashMap<>();
            response.put("token", newAccessToken);
            response.put("type", "Bearer");
            response.put("email", userAccount.getEmail());
            response.put("expiresIn", jwtExpirationMs / 1000);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Log de falha - erro ao processar
            auditLogService.logTokenRefreshFailure("unknown", ipAddress, userAgent, 
                                                  "/api/auth/refresh");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Erro ao processar refresh token: " + e.getMessage()));
        }
    }

    /**
     * Logout endpoint - revoga refresh token
     */
    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revoga o refresh token")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> request,
                                   HttpServletRequest httpRequest) {
        String refreshToken = request.get("refreshToken");
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = getClientUserAgent(httpRequest);

        try {
            // Revogar refresh token e capturar informações do usuário
            String userEmail = "unknown";
            String userId = null;
            
            if (refreshToken != null && !refreshToken.isEmpty()) {
                // Validar e buscar informações antes de revogar
                Optional<RefreshTokenEntity> validToken = refreshTokenService.validateRefreshToken(refreshToken);
                if (validToken.isPresent()) {
                    RefreshTokenEntity rt = validToken.get();
                    userId = rt.getUserId();
                    
                    // Buscar email do usuário
                    Optional<UserAccountEntity> user = userRepository.findByUserId(userId);
                    if (user.isPresent()) {
                        userEmail = user.get().getEmail();
                    }
                }
                
                refreshTokenService.revokeRefreshToken(refreshToken);
            }

            // Log de sucesso - logout realizado
            if (userId != null) {
                auditLogService.logLogout(userId, userEmail, ipAddress, userAgent, "/api/auth/logout");
            }

            return ResponseEntity.ok(Map.of("message", "Logout realizado com sucesso"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erro ao processar logout: " + e.getMessage()));
        }
    }

    /**
     * Valida um token JWT
     */
    @GetMapping("/validate")
    @Operation(summary = "Validar token", description = "Valida um token JWT")
    public ResponseEntity<?> validateToken(@RequestParam String token) {
        try {
            if (jwtProvider.validateToken(token)) {
                String username = jwtProvider.getUsernameFromToken(token);
                Set<String> roles = jwtProvider.getRolesFromToken(token);

                Map<String, Object> response = Map.of(
                        "valid", true,
                        "email", username,
                        "roles", roles
                );

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("valid", false, "error", "Token inválido ou expirado"));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("valid", false, "error", e.getMessage()));
        }
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Verifica saúde do serviço de autenticação")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "OK"));
    }

    /**
     * Extrai endereço IP do cliente da requisição HTTP
     * Suporta proxies e load balancers (X-Forwarded-For, X-Real-IP)
     */
    private String getClientIpAddress(HttpServletRequest request) {
        // Verificar header X-Forwarded-For (usado por proxies e load balancers)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Pode ter múltiplos IPs, pegar o primeiro
            return xForwardedFor.split(",")[0].trim();
        }

        // Verificar header X-Real-IP (alternativa)
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        // Usar IP remoto direto
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null ? remoteAddr : "UNKNOWN";
    }

    /**
     * Extrai User-Agent do cliente
     */
    private String getClientUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null && !userAgent.isEmpty() ? userAgent : "UNKNOWN";
    }
}
