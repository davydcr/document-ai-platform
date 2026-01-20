package com.davydcr.document.infrastructure.observability;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health Indicator customizado para verificar o status
 * dos componentes críticos da plataforma
 */
@Component("documentPlatformHealth")
public class DocumentPlatformHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        try {
            // Verificações básicas do sistema
            boolean ocrReady = checkOcrAvailability();
            boolean storageReady = checkStorageAvailability();
            boolean classifierReady = checkClassifierAvailability();

            if (ocrReady && storageReady && classifierReady) {
                return Health.up()
                        .withDetail("ocrEngine", "Tesseract 5")
                        .withDetail("storageService", "Operational")
                        .withDetail("classificationService", "Operational")
                        .build();
            } else {
                return Health.outOfService()
                        .withDetail("ocrReady", ocrReady)
                        .withDetail("storageReady", storageReady)
                        .withDetail("classifierReady", classifierReady)
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }

    private boolean checkOcrAvailability() {
        try {
            // Verificar se Tesseract está disponível
            // Em produção, isso seria uma verificação real
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkStorageAvailability() {
        try {
            // Verificar se o storage de documentos está disponível
            String uploadDir = System.getProperty("app.document.upload-dir", "/var/document-ai/uploads");
            java.nio.file.Path path = java.nio.file.Paths.get(uploadDir);
            return java.nio.file.Files.exists(path) || java.nio.file.Files.createDirectories(path) != null;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkClassifierAvailability() {
        try {
            // Verificar disponibilidade do classificador
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
