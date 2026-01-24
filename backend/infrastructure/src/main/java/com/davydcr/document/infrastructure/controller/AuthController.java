package com.davydcr.document.infrastructure.controller;

import com.davydcr.document.application.dto.LoginRequest;
import com.davydcr.document.application.dto.LoginResponse;
import com.davydcr.document.infrastructure.persistence.entity.UserAccountEntity;
import com.davydcr.document.infrastructure.repository.UserRepository;
import com.davydcr.document.infrastructure.security.JwtProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Authentication Controller - Endpoint para login com JWT
 */
@RestController
@RequestMapping("/auth")
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
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Optional<UserAccountEntity> user = userRepository.findByEmail(request.username());
        
        if (user.isEmpty() || !user.get().getActive()) {
            return ResponseEntity.status(401).body(Map.of("error", "Credenciais inválidas"));
        }

        UserAccountEntity userAccount = user.get();
        
        // Validar senha com BCrypt
        if (!passwordEncoder.matches(request.password(), userAccount.getPasswordHash())) {
            return ResponseEntity.status(401).body(Map.of("error", "Credenciais inválidas"));
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
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "OK"));
    }
}
