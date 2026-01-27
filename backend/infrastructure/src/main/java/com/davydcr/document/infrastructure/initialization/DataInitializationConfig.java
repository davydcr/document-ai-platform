package com.davydcr.document.infrastructure.initialization;

import com.davydcr.document.infrastructure.persistence.entity.RoleEntity;
import com.davydcr.document.infrastructure.persistence.entity.UserAccountEntity;
import com.davydcr.document.infrastructure.repository.RoleRepository;
import com.davydcr.document.infrastructure.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

/**
 * Inicialização com dados de seed para desenvolvimento e testes
 * Implementa proper authentication schema seeding
 * TODO: Integrar com PasswordEncoder quando Spring Security estiver configurado
 */
@Configuration
public class DataInitializationConfig {

    @Bean
    public CommandLineRunner initializeData(
            UserRepository userRepository,
            RoleRepository roleRepository) {
        return args -> {
            // Criar roles padrão
            createDefaultRoles(roleRepository);
            
            // Criar usuário padrão para testes
            createDefaultUser(userRepository, roleRepository);
        };
    }

    /**
     * Cria roles padrão se não existirem.
     */
    private void createDefaultRoles(RoleRepository roleRepository) {
        String[] defaultRoles = {"ROLE_USER", "ROLE_ADMIN", "ROLE_API_CLIENT"};
        
        for (String roleName : defaultRoles) {
            Optional<RoleEntity> existingRole = roleRepository.findByName(roleName);
            if (existingRole.isEmpty()) {
                RoleEntity role = new RoleEntity();
                role.setName(roleName);
                role.setDescription(getRoleDescription(roleName));
                roleRepository.save(role);
            }
        }
    }

    /**
     * Cria usuário padrão para testes e desenvolvimento.
     * TODO: Usar PasswordEncoder após configurar Spring Security
     */
    private void createDefaultUser(UserRepository userRepository, RoleRepository roleRepository) {
        // Verificar se usuário padrão já existe
        Optional<UserAccountEntity> existingUser = userRepository.findByEmail("admin@example.com");
        if (existingUser.isPresent()) {
            return;
        }

        // Buscar role USER
        RoleEntity userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new IllegalStateException("ROLE_USER not found"));

        // Criar usuário
        UserAccountEntity admin = new UserAccountEntity();
        admin.setId("default-user-001");
        admin.setEmail("admin@example.com");
        admin.setFirstName("Admin");
        admin.setLastName("System");
        // TODO: Usar PasswordEncoder após configurar Spring Security
        admin.setPasswordHash("admin123");
        admin.setActive(true);

        // Adicionar role
        admin.getRoles().add(userRole);

        userRepository.save(admin);
    }

    /**
     * Retorna descrição da role.
     */
    private String getRoleDescription(String roleName) {
        return switch (roleName) {
            case "ROLE_USER" -> "Usuário comum da plataforma";
            case "ROLE_ADMIN" -> "Administrador da plataforma";
            case "ROLE_API_CLIENT" -> "Cliente de API integrado";
            default -> "Role não definido";
        };
    }
}
