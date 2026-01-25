package com.davydcr.document.infrastructure.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Serviço para disparar eventos via WebSocket.
 * Envia atualizações em tempo real para clientes conectados.
 */
@Service
public class DocumentWebSocketService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentWebSocketService.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public DocumentWebSocketService(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Broadcast evento de mudança de documento para todos os subscribers de /topic/documents/all
     */
    public void broadcastDocumentStatusChange(String documentId, String status, String eventType) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("documentId", documentId);
            message.put("status", status);
            message.put("eventType", eventType);
            message.put("timestamp", System.currentTimeMillis());

            logger.info("Broadcasting document status change: documentId={}, status={}, eventType={}",
                    documentId, status, eventType);

            messagingTemplate.convertAndSend("/topic/documents/all", message);
        } catch (Exception e) {
            logger.error("Error broadcasting document status change", e);
        }
    }

    /**
     * Envia notificação específica para um documento via /topic/documents/{id}
     */
    public void sendDocumentUpdate(String documentId, String status, Map<String, Object> data) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("documentId", documentId);
            message.put("status", status);
            message.put("data", data);
            message.put("timestamp", System.currentTimeMillis());

            logger.info("Sending document update: documentId={}, status={}", documentId, status);

            messagingTemplate.convertAndSend("/topic/documents/" + documentId, message);
        } catch (Exception e) {
            logger.error("Error sending document update", e);
        }
    }

    /**
     * Envia notificação de error para um documento específico
     */
    public void sendDocumentError(String documentId, String errorMessage) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("documentId", documentId);
            message.put("errorMessage", errorMessage);
            message.put("timestamp", System.currentTimeMillis());

            logger.error("Sending document error: documentId={}, error={}", documentId, errorMessage);

            messagingTemplate.convertAndSend("/topic/documents/" + documentId, message);
        } catch (Exception e) {
            logger.error("Error sending document error notification", e);
        }
    }

    /**
     * Envia notificação de progresso de processamento
     */
    public void sendProcessingProgress(String documentId, int progress, String phase) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("documentId", documentId);
            message.put("progress", progress);
            message.put("phase", phase);
            message.put("timestamp", System.currentTimeMillis());

            logger.info("Sending processing progress: documentId={}, progress={}, phase={}",
                    documentId, progress, phase);

            messagingTemplate.convertAndSend("/topic/documents/" + documentId + "/progress", message);
        } catch (Exception e) {
            logger.error("Error sending processing progress", e);
        }
    }
}
