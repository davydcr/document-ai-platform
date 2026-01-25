package com.davydcr.document.infrastructure.service;

import com.davydcr.document.infrastructure.persistence.entity.WebhookDeliveryAttemptEntity;
import com.davydcr.document.infrastructure.persistence.entity.WebhookSubscriptionEntity;
import com.davydcr.document.infrastructure.persistence.repository.WebhookDeliveryAttemptJpaRepository;
import com.davydcr.document.infrastructure.persistence.repository.WebhookSubscriptionJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Serviço para entregar webhooks via HTTP POST.
 * Implementa retry logic e persistência de tentativas.
 */
@Service
public class WebhookDeliveryService {

    private static final Logger logger = LoggerFactory.getLogger(WebhookDeliveryService.class);
    private static final int MAX_ATTEMPTS = 3;
    private static final int INITIAL_RETRY_DELAY_SECONDS = 5;

    private final WebhookSubscriptionJpaRepository webhookRepository;
    private final WebhookDeliveryAttemptJpaRepository deliveryAttemptRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public WebhookDeliveryService(
            WebhookSubscriptionJpaRepository webhookRepository,
            WebhookDeliveryAttemptJpaRepository deliveryAttemptRepository,
            ObjectMapper objectMapper) {
        this.webhookRepository = webhookRepository;
        this.deliveryAttemptRepository = deliveryAttemptRepository;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();
    }

    /**
     * Executa entrega de webhook para um evento.
     * Cria registro de tentativa e implementa retry logic.
     */
    public void deliverWebhook(String webhookId, String eventType, Map<String, Object> eventPayload) {
        try {
            logger.info("Starting webhook delivery: webhookId={}, eventType={}", webhookId, eventType);

            WebhookSubscriptionEntity webhook = webhookRepository.findById(webhookId)
                    .orElse(null);

            if (webhook == null) {
                logger.warn("Webhook not found: {}", webhookId);
                return;
            }

            if (!webhook.getActive()) {
                logger.warn("Webhook is inactive: {}", webhookId);
                return;
            }

            // Serializar payload para JSON
            String jsonPayload = objectMapper.writeValueAsString(eventPayload);

            // Criar tentativa de entrega
            WebhookDeliveryAttemptEntity attempt = new WebhookDeliveryAttemptEntity(
                    webhook.getId(),
                    eventType,
                    jsonPayload
            );

            // Realizar HTTP POST
            HttpResponse<String> response = performHttpPost(webhook.getUrl(), jsonPayload);

            // Atualizar tentativa com resultado
            attempt.setHttpStatusCode(response.statusCode());
            attempt.setResponseBody(response.body());
            attempt.setSuccess(response.statusCode() >= 200 && response.statusCode() < 300);
            attempt.setAttemptedAt(Instant.now());

            // Se falhou, agendar retry
            if (!attempt.isSuccess()) {
                attempt.setNextRetryAt(calculateNextRetryTime(attempt.getAttemptNumber()));
                webhook.incrementFailureCount();
            } else {
                webhook.resetFailureCount();
            }

            // Persistir tentativa
            deliveryAttemptRepository.save(attempt);
            webhookRepository.save(webhook);

            logger.info("Webhook delivery completed: webhookId={}, statusCode={}, success={}",
                    webhookId, response.statusCode(), attempt.isSuccess());

        } catch (Exception e) {
            logger.error("Error delivering webhook: webhookId={}", webhookId, e);
        }
    }

    /**
     * Executa HTTP POST para URL do webhook.
     */
    private HttpResponse<String> performHttpPost(String url, String jsonPayload) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new java.net.URI(url))
                .header("Content-Type", "application/json")
                .header("X-Webhook-ID", UUID.randomUUID().toString())
                .header("X-Webhook-Timestamp", Instant.now().toString())
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .timeout(java.time.Duration.ofSeconds(10))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Calcula tempo da próxima tentativa com exponential backoff.
     * 2^attempt * 5 segundos
     */
    private Instant calculateNextRetryTime(int attemptNumber) {
        long delaySeconds = (long) Math.pow(2, attemptNumber) * INITIAL_RETRY_DELAY_SECONDS;
        return Instant.now().plus(delaySeconds, ChronoUnit.SECONDS);
    }

    /**
     * Processa tentativas que falharam e devem ser reenviadas.
     * Chamado pelo ProcessWebhookEventHandler.
     */
    public void processFailedDeliveryAttempts(String webhookId, String eventType) {
        try {
            WebhookDeliveryAttemptEntity lastAttempt = deliveryAttemptRepository
                    .findLastFailedAttempt(webhookId, eventType)
                    .orElse(null);

            if (lastAttempt == null || lastAttempt.getAttemptNumber() >= MAX_ATTEMPTS) {
                logger.warn("Max retry attempts reached or no failed attempts: webhookId={}, eventType={}",
                        webhookId, eventType);
                return;
            }

            logger.info("Retrying webhook delivery: webhookId={}, eventType={}, attemptNumber={}",
                    webhookId, eventType, lastAttempt.getAttemptNumber() + 1);

            // Criar nova tentativa
            WebhookDeliveryAttemptEntity newAttempt = new WebhookDeliveryAttemptEntity(
                    webhookId,
                    eventType,
                    lastAttempt.getEventPayload()
            );

            // Copiar dados básicos
            newAttempt.setAttemptNumber(lastAttempt.getAttemptNumber() + 1);

            // Fazer HTTP POST
            HttpResponse<String> response = performHttpPost(
                    webhookRepository.findById(webhookId).orElseThrow().getUrl(),
                    lastAttempt.getEventPayload()
            );

            // Atualizar resultado
            newAttempt.setHttpStatusCode(response.statusCode());
            newAttempt.setResponseBody(response.body());
            newAttempt.setSuccess(response.statusCode() >= 200 && response.statusCode() < 300);
            newAttempt.setAttemptedAt(Instant.now());

            if (!newAttempt.isSuccess() && newAttempt.getAttemptNumber() < MAX_ATTEMPTS) {
                newAttempt.setNextRetryAt(calculateNextRetryTime(newAttempt.getAttemptNumber()));
            }

            deliveryAttemptRepository.save(newAttempt);

            logger.info("Webhook retry completed: webhookId={}, statusCode={}, success={}",
                    webhookId, response.statusCode(), newAttempt.isSuccess());

        } catch (Exception e) {
            logger.error("Error processing failed webhook deliveries: webhookId={}", webhookId, e);
        }
    }
}
