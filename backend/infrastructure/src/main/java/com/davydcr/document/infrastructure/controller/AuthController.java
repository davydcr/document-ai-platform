package com.davydcr.document.infrastructure.controller;

import com.davydcr.document.application.dto.LoginRequest;
import com.davydcr.document.application.dto.LoginResponse;
import com.davydcr.document.infrastructure.persistence.entity.UserAccountEntity;
import com.davydcr.document.infrastructure.repository.UserRepository;
import com.davydcr.document.infrastructure.security.JwtProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Authentication Controller - Endpoint para login com JWT
 * Fornece autenticação e validação de tokens
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Endpoints de autenticação e autorização")
@CrossOrigin(origins = "*")
public class AuthController {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.jwt.expiration:86400000}")
    private long jwtExpirationMs;

    public AuthController(JwtProvider jwtProvider, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.jwtProvider = jwtProvider;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Login endpoint - retorna JWT token
     * Autentica com credenciais do banco de dados
     */
    @PostMapping("/login")
    @Operation(summary = "Login", description = "Faz login com email e senha, retorna JWT token")
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
        
        return ResponseEntity.ok(LoginResponse.bearer(
                token,
                userAccount.getEmail(),
                roles,
                jwtExpirationMs / 1000
        ));
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
