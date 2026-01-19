package com.davydcr.document.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuração do OpenAPI (Swagger) para a API do Document AI Platform
 * Documentação disponível em: /swagger-ui.html
 * OpenAPI JSON em: /v3/api-docs
 */
@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Document AI Platform API")
                .version("1.0.0")
                .description("API para processamento automático de documentos com IA\n\n" +
                    "Funcionalidades principais:\n" +
                    "- Upload e armazenamento de documentos (PDF, IMG, TXT)\n" +
                    "- Classificação automática com modelos de IA\n" +
                    "- Extração de conteúdo com OCR\n" +
                    "- Listagem e recuperação de documentos processados\n\n" +
                    "**Base URL**: `/api`")
                .contact(new Contact()
                    .name("Document AI Team")
                    .email("support@document-ai.local")
                    .url("https://document-ai-platform.local"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080/api")
                    .description("Local Development Server"),
                new Server()
                    .url("https://api.document-ai-platform.local/api")
                    .description("Production Server"),
                new Server()
                    .url("https://staging-api.document-ai-platform.local/api")
                    .description("Staging Server")
            ));
    }
}
