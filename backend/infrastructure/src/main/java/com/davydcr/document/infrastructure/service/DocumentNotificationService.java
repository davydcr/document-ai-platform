package com.davydcr.document.infrastructure.service;

import com.davydcr.document.application.dto.ProcessDocumentOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serviço para notificações de webhook quando documentos completam.
 * 
 * Permite que clientes se registrem para receber notificações de completion
 * via webhook quando documento termina de processar.
 */
@Service
public class DocumentNotificationService {

    private static final Logger log = LoggerFactory.getLogger(DocumentNotificationService.class);

    // Mapa: documentId -> webhookUrl
    private final Map<String, String> webhookSubscriptions = new ConcurrentHashMap<>();
    
    private final RestTemplate restTemplate;

    @Autowired
    public DocumentNotificationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Registra um webhook para ser notificado quando documento completar.
     * 
     * @param documentId ID do documento
     * @param webhookUrl URL para receber notificação (POST)
     */
    public void registerWebhook(String documentId, String webhookUrl) {
        if (documentId == null || webhookUrl == null) {
            log.warn("Tentativa de registrar webhook inválido");
            return;
        }
        
        webhookSubscriptions.put(documentId, webhookUrl);
        log.info("Webhook registrado para documento {}: {}", documentId, webhookUrl);
    }

    /**
     * Desregistra webhook para um documento.
     * 
     * @param documentId ID do documento
     */
    public void unregisterWebhook(String documentId) {
        webhookSubscriptions.remove(documentId);
        log.info("Webhook removido para documento {}", documentId);
    }

    /**
     * Notifica via webhook quando documento é completado.
     * 
     * @param documentId ID do documento
     * @param result Resultado do processamento
     */
    public void notifyCompletion(String documentId, ProcessDocumentOutput result) {
        String webhookUrl = webhookSubscriptions.get(documentId);
        
        if (webhookUrl == null) {
            log.debug("Nenhum webhook registrado para documento {}", documentId);
            return;
        }

        try {
            // Preparar payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("event", "document.completed");
            payload.put("documentId", documentId);
            payload.put("status", result.getStatus());
            payload.put("classification", result.getClassification());
            payload.put("extractedText", result.getExtractedTextPreview());
            payload.put("confidence", result.getConfidencePercentage());
            payload.put("timestamp", System.currentTimeMillis());

            // Enviar notificação
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            restTemplate.postForObject(webhookUrl, request, String.class);
            log.info("Webhook enviado com sucesso para documento {}", documentId);
            
            // Remover subscription após notificar
            webhookSubscriptions.remove(documentId);

        } catch (Exception e) {
            log.error("Erro ao enviar webhook para documento {}: {}", documentId, e.getMessage());
            // Não falhar o processamento do documento por erro de webhook
        }
    }

    /**
     * Notifica erro durante processamento.
     * 
     * @param documentId ID do documento
     * @param errorMessage Mensagem de erro
     */
    public void notifyFailure(String documentId, String errorMessage) {
        String webhookUrl = webhookSubscriptions.get(documentId);
        
        if (webhookUrl == null) {
            return;
        }

        try {
            // Preparar payload de erro
            Map<String, Object> payload = new HashMap<>();
            payload.put("event", "document.failed");
            payload.put("documentId", documentId);
            payload.put("status", "FAILED");
            payload.put("error", errorMessage);
            payload.put("timestamp", System.currentTimeMillis());

            // Enviar notificação
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            restTemplate.postForObject(webhookUrl, request, String.class);
            log.info("Webhook de falha enviado para documento {}", documentId);
            
            // Remover subscription
            webhookSubscriptions.remove(documentId);

        } catch (Exception e) {
            log.error("Erro ao enviar webhook de falha para documento {}: {}", documentId, e.getMessage());
        }
    }

    /**
     * Obtém número de webhooks ativos.
     * 
     * @return Quantidade de subscriptions ativas
     */
    public int getActiveSubscriptionCount() {
        return webhookSubscriptions.size();
    }
}
