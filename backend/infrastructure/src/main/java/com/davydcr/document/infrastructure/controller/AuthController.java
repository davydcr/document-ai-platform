package com.davydcr.document.infrastructure.controller;

import com.davydcr.document.application.dto.LoginRequest;
import com.davydcr.document.application.dto.LoginResponse;
import com.davydcr.document.infrastructure.persistence.entity.RefreshTokenEntity;
import com.davydcr.document.infrastructure.persistence.entity.UserAccountEntity;
import com.davydcr.document.infrastructure.repository.UserRepository;
import com.davydcr.document.infrastructure.security.JwtProvider;
import com.davydcr.document.infrastructure.security.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    @Value("${app.jwt.expiration:86400000}")
    private long jwtExpirationMs;

    public AuthController(JwtProvider jwtProvider, RefreshTokenService refreshTokenService, 
                         UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.jwtProvider = jwtProvider;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Login endpoint - retorna JWT token e refresh token
     * Autentica com credenciais do banco de dados
     */
    @PostMapping("/login")
    @Operation(summary = "Login", description = "Faz login com email e senha, retorna JWT token e refresh token")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Optional<UserAccountEntity> user = userRepository.findByEmail(request.username());
        
        if (user.isEmpty() || !user.get().getActive()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciais inválidas"));
        }

        UserAccountEntity userAccount = user.get();
        
        // Validar senha com BCrypt
        if (!passwordEncoder.matches(request.password(), userAccount.getPasswordHash())) {
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
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Refresh token é obrigatório"));
        }

        try {
            // Validar refresh token
            Optional<RefreshTokenEntity> validToken = refreshTokenService.validateRefreshToken(refreshToken);

            if (validToken.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Refresh token inválido ou expirado"));
            }

            RefreshTokenEntity rt = validToken.get();
            
            // Buscar usuário
            Optional<UserAccountEntity> user = userRepository.findByUserId(rt.getUserId());
            if (user.isEmpty() || !user.get().getActive()) {
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

            Map<String, Object> response = new HashMap<>();
            response.put("token", newAccessToken);
            response.put("type", "Bearer");
            response.put("email", userAccount.getEmail());
            response.put("expiresIn", jwtExpirationMs / 1000);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Erro ao processar refresh token: " + e.getMessage()));
        }
    }

    /**
     * Logout endpoint - revoga refresh token
     */
    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revoga o refresh token")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        if (refreshToken != null && !refreshToken.isEmpty()) {
            refreshTokenService.revokeRefreshToken(refreshToken);
        }

        return ResponseEntity.ok(Map.of("message", "Logout realizado com sucesso"));
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
}
