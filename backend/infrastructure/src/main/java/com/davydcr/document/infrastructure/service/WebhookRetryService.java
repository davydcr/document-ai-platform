package com.davydcr.document.infrastructure.service;

import com.davydcr.document.infrastructure.persistence.entity.WebhookDeliveryAttemptEntity;
import com.davydcr.document.infrastructure.persistence.entity.WebhookSubscriptionEntity;
import com.davydcr.document.infrastructure.persistence.repository.WebhookDeliveryAttemptJpaRepository;
import com.davydcr.document.infrastructure.persistence.repository.WebhookSubscriptionJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Serviço para retentar entregas de webhook que falharam.
 * Utiliza exponential backoff e limite máximo de tentativas.
 */
@Service
public class WebhookRetryService {

    private static final Logger logger = LoggerFactory.getLogger(WebhookRetryService.class);
    private static final int MAX_RETRY_ATTEMPTS = 10;
    private static final int MAX_FAILURE_COUNT_BEFORE_DISABLE = 10;

    private final WebhookSubscriptionJpaRepository webhookRepository;
    private final WebhookDeliveryAttemptJpaRepository deliveryAttemptRepository;
    private final WebhookDeliveryService deliveryService;

    public WebhookRetryService(
            WebhookSubscriptionJpaRepository webhookRepository,
            WebhookDeliveryAttemptJpaRepository deliveryAttemptRepository,
            WebhookDeliveryService deliveryService) {
        this.webhookRepository = webhookRepository;
        this.deliveryAttemptRepository = deliveryAttemptRepository;
        this.deliveryService = deliveryService;
    }

    /**
     * Processa webhooks com falha que estão prontos para retry.
     * Executado a cada 30 segundos.
     * Implementa exponential backoff: aguarda 2^attempt * 5 segundos entre tentativas.
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 60000)  // 30s delay, 60s initial
    public void processFailedWebhookAttempts() {
        try {
            logger.info("Starting failed webhook retry processing");

            // Buscar tentativas que falharam e estão prontas para retry
            List<WebhookDeliveryAttemptEntity> failedAttempts =
                    deliveryAttemptRepository.findBySuccessFalseAndNextRetryAtIsNotNullAndNextRetryAtLessThanEqual(
                            Instant.now());

            logger.info("Found {} failed webhook attempts ready for retry", failedAttempts.size());

            for (WebhookDeliveryAttemptEntity attempt : failedAttempts) {
                try {
                    processRetryAttempt(attempt);
                } catch (Exception e) {
                    logger.error("Error processing retry for attempt: {}", attempt.getId(), e);
                }
            }

            logger.info("Completed failed webhook retry processing");

        } catch (Exception e) {
            logger.error("Error in webhook retry scheduled task", e);
        }
    }

    /**
     * Processa uma tentativa de retry individual.
     */
    private void processRetryAttempt(WebhookDeliveryAttemptEntity attempt) {
        logger.info("Processing retry for webhook: webhookId={}, attemptNumber={}, eventType={}",
                attempt.getWebhookSubscriptionId(), attempt.getAttemptNumber(), attempt.getEventType());

        // Verificar se atingiu o limite de tentativas
        if (attempt.getAttemptNumber() >= MAX_RETRY_ATTEMPTS) {
            logger.warn("Max retry attempts reached: webhookId={}, eventType={}",
                    attempt.getWebhookSubscriptionId(), attempt.getEventType());

            // Desativar webhook se atingiu muitas falhas consecutivas
            WebhookSubscriptionEntity webhook = webhookRepository
                    .findById(attempt.getWebhookSubscriptionId())
                    .orElse(null);

            if (webhook != null && webhook.getFailureCount() >= MAX_FAILURE_COUNT_BEFORE_DISABLE) {
                logger.warn("Disabling webhook due to repeated failures: webhookId={}", webhook.getId());
                webhook.setActive(false);
                webhookRepository.save(webhook);
            }

            return;
        }

        // Delegar retry para WebhookDeliveryService
        deliveryService.processFailedDeliveryAttempts(
                attempt.getWebhookSubscriptionId(),
                attempt.getEventType());
    }

    /**
     * Limpa tentativas antigas e bem-sucedidas.
     * Executado diariamente às 2:00 AM.
     * Remove tentativas bem-sucedidas com mais de 30 dias.
     */
    @Scheduled(cron = "0 0 2 * * ?")  // 2:00 AM daily
    public void cleanupOldSuccessfulAttempts() {
        try {
            logger.info("Starting cleanup of old successful webhook attempts");

            // Calcular a data limite: 30 dias atrás
            Instant thirtyDaysAgo = Instant.now().minus(java.time.Duration.ofDays(30));

            // Buscar tentativas bem-sucedidas com mais de 30 dias
            List<WebhookDeliveryAttemptEntity> oldSuccessfulAttempts = 
                    deliveryAttemptRepository.findOldSuccessfulAttempts(thirtyDaysAgo);

            if (!oldSuccessfulAttempts.isEmpty()) {
                logger.info("Found {} old successful webhook attempts to delete", oldSuccessfulAttempts.size());
                
                // Deletar as tentativas antigas
                deliveryAttemptRepository.deleteAll(oldSuccessfulAttempts);
                
                logger.info("Successfully deleted {} old webhook attempts", oldSuccessfulAttempts.size());
            } else {
                logger.info("No old successful webhook attempts to delete");
            }

        } catch (Exception e) {
            logger.error("Error in webhook cleanup scheduled task", e);
        }
    }
}
