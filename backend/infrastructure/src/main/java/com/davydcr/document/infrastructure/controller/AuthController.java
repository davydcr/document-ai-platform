package com.davydcr.document.infrastructure.controller;

import com.davydcr.document.application.dto.LoginRequest;
import com.davydcr.document.application.dto.LoginResponse;
import com.davydcr.document.infrastructure.security.JwtProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Authentication Controller - Endpoint para login com JWT
 */
@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final JwtProvider jwtProvider;

    @Value("${app.jwt.expiration:86400000}")
    private long jwtExpirationMs;

    public AuthController(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    /**
     * Login endpoint - retorna JWT token
     * Demo credentials: admin/admin123, user/user123
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (authenticate(request.username(), request.password())) {
            UUID userId = UUID.randomUUID();
            Set<String> roles = getRoles(request.username());
            String token = jwtProvider.generateToken(userId, request.username(), roles);
            
            return ResponseEntity.ok(LoginResponse.bearer(
                    token,
                    request.username(),
                    roles,
                    jwtExpirationMs / 1000
            ));
        }
        return ResponseEntity.status(401).body(Map.of("error", "Credenciais inválidas"));
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "OK"));
    }

    private boolean authenticate(String username, String password) {
        return ("admin".equals(username) && "admin123".equals(password))
                || ("user".equals(username) && "user123".equals(password));
    }

    private Set<String> getRoles(String username) {
        if ("admin".equals(username)) {
            return Set.of("ADMIN", "USER");
        }
        return Set.of("USER");
    }
}
