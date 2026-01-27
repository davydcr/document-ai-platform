package com.davydcr.document.infrastructure.initialization;

import com.davydcr.document.infrastructure.repository.RoleRepository;
import com.davydcr.document.infrastructure.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Inicialização com dados de seed para desenvolvimento e testes
 * DISABLED: Use proper migration files instead
 */
@Configuration
public class DataInitializationConfig {

    @Bean
    public CommandLineRunner initializeData(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            // Data initialization disabled for Docker deployment
            // TODO: Implement proper authentication schema seeding
        };
    }
}
