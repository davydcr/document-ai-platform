package com.davydcr.document.infrastructure.event;

import com.davydcr.document.infrastructure.persistence.repository.WebhookSubscriptionJpaRepository;
import com.davydcr.document.infrastructure.service.WebhookDeliveryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Listener para processar eventos de documentos e disparar webhooks.
 * Consome eventos da fila "webhook-events-queue" e triga callbacks registrados.
 */
@Component
public class ProcessWebhookEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(ProcessWebhookEventHandler.class);

    private final WebhookSubscriptionJpaRepository webhookRepository;
    private final WebhookDeliveryService webhookDeliveryService;
    private final DocumentWebSocketService documentWebSocketService;
    private final ObjectMapper objectMapper;

    public ProcessWebhookEventHandler(
            WebhookSubscriptionJpaRepository webhookRepository,
            WebhookDeliveryService webhookDeliveryService,
            DocumentWebSocketService documentWebSocketService,
            ObjectMapper objectMapper) {
        this.webhookRepository = webhookRepository;
        this.webhookDeliveryService = webhookDeliveryService;
        this.documentWebSocketService = documentWebSocketService;
        this.objectMapper = objectMapper;
    }

    /**
     * Processa eventos de mudança de estado de documento.
     * Dispara webhooks registrados para o evento específico.
     */
    @RabbitListener(queues = "webhook-events-queue")
    public void handleDocumentEvent(String message) {
        try {
            logger.info("Received webhook event: {}", message);

            // Parse do evento JSON
            Map<String, Object> eventData = objectMapper.readValue(message, Map.class);
            String eventType = (String) eventData.get("eventType");
            String documentId = (String) eventData.get("documentId");

            logger.info("Processing webhook for event: eventType={}, documentId={}", eventType, documentId);

            // Buscar webhooks ativos que escutam este evento
            webhookRepository.findActiveWebhooksForEvent(eventType).forEach(webhook -> {
                logger.info("Triggering webhook: webhookId={}, eventType={}", webhook.getId(), eventType);

                try {
                    // Construir payload com dados do evento
                    Map<String, Object> payload = buildWebhookPayload(eventData);

                    // Disparar entrega assíncrona
                    webhookDeliveryService.deliverWebhook(webhook.getId(), eventType, payload);

                } catch (Exception e) {
                    logger.error("Error processing webhook: webhookId={}, eventType={}", webhook.getId(), eventType, e);
                }
            });

            // Broadcast do evento via WebSocket para atualizações em tempo real
            String status = (String) eventData.get("status");
            documentWebSocketService.broadcastDocumentStatusChange(documentId, status, eventType);

        } catch (Exception e) {
            logger.error("Error handling webhook event", e);
        }
    }

    /**
     * Constrói payload para webhook a partir dos dados do evento.
     */
    private Map<String, Object> buildWebhookPayload(Map<String, Object> eventData) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", eventData.get("documentId"));
        payload.put("eventType", eventData.get("eventType"));
        payload.put("timestamp", Instant.now());
        payload.put("status", eventData.get("status"));
        payload.put("eventData", eventData);
        return payload;
    }
}
