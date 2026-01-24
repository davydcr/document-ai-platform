package com.davydcr.document.infrastructure.initialization;

import com.davydcr.document.infrastructure.persistence.entity.RoleEntity;
import com.davydcr.document.infrastructure.persistence.entity.UserAccountEntity;
import com.davydcr.document.infrastructure.repository.RoleRepository;
import com.davydcr.document.infrastructure.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

/**
 * Inicialização com dados de seed para desenvolvimento e testes
 */
@Configuration
public class DataInitializationConfig {

    @Bean
    public CommandLineRunner initializeData(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            // Verificar se dados já foram criados
            if (userRepository.count() > 0) {
                return;
            }

            // Criar roles
            RoleEntity adminRole = roleRepository.save(new RoleEntity("ADMIN", "Administrator with full access"));
            RoleEntity userRole = roleRepository.save(new RoleEntity("USER", "Standard user access"));
            RoleEntity analystRole = roleRepository.save(new RoleEntity("ANALYST", "Data analyst access"));

            // Criar usuário admin
            UserAccountEntity admin = new UserAccountEntity();
            admin.setEmail("admin@example.com");
            admin.setFirstName("Admin");
            admin.setLastName("User");
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            admin.setActive(true);
            admin.setRoles(Set.of(adminRole, userRole));
            userRepository.save(admin);

            // Criar usuário padrão
            UserAccountEntity standardUser = new UserAccountEntity();
            standardUser.setEmail("user@example.com");
            standardUser.setFirstName("John");
            standardUser.setLastName("Doe");
            standardUser.setPasswordHash(passwordEncoder.encode("user123"));
            standardUser.setActive(true);
            standardUser.setRoles(Set.of(userRole));
            userRepository.save(standardUser);

            // Criar usuário analista
            UserAccountEntity analyst = new UserAccountEntity();
            analyst.setEmail("analyst@example.com");
            analyst.setFirstName("Jane");
            analyst.setLastName("Smith");
            analyst.setPasswordHash(passwordEncoder.encode("analyst123"));
            analyst.setActive(true);
            analyst.setRoles(Set.of(analystRole, userRole));
            userRepository.save(analyst);

            System.out.println("✅ Dados de seed inicializados com sucesso!");
            System.out.println("   - 3 Roles criadas");
            System.out.println("   - 3 Usuários criados");
            System.out.println("   Credenciais:");
            System.out.println("   - admin@example.com / admin123");
            System.out.println("   - user@example.com / user123");
            System.out.println("   - analyst@example.com / analyst123");
        };
    }
}
