package com.davydcr.document.infrastructure.service;

import com.davydcr.document.application.dto.DocumentProcessingMessage;
import com.davydcr.document.infrastructure.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Serviço para enviar mensagens de processamento de documentos para fila RabbitMQ
 * Implementa padrão de producer para processamento assíncrono (Semana 7)
 */
@Service
@ConditionalOnProperty(
    name = "app.async.processing-enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class AsyncProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(AsyncProcessingService.class);

    private final RabbitTemplate rabbitTemplate;

    public AsyncProcessingService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        logger.info("AsyncProcessingService initialized with RabbitMQ");
    }

    /**
     * Envia mensagem de processamento para fila
     * Retorna true se enviada com sucesso
     */
    public void sendProcessingMessage(DocumentProcessingMessage message) {
        try {
            logger.info("Sending document to processing queue: documentId={}, fileType={}", 
                    message.getDocumentId(), message.getFileType());

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.DOCUMENT_EXCHANGE,
                RabbitMQConfig.DOCUMENT_PROCESS_ROUTING_KEY,
                message,
                msg -> {
                    // Adiciona headers customizados
                    msg.getMessageProperties().getHeaders()
                        .put("documentId", message.getDocumentId());
                    msg.getMessageProperties().getHeaders()
                        .put("timestamp", System.currentTimeMillis());
                    return msg;
                }
            );

            logger.debug("Message sent successfully to queue for documentId: {}", 
                    message.getDocumentId());

        } catch (Exception e) {
            logger.error("Error sending document to processing queue: documentId={}", 
                    message.getDocumentId(), e);
            throw new RuntimeException("Failed to queue document for processing", e);
        }
    }

    /**
     * Verifica se o serviço está ativo/disponível
     */
    public boolean isAvailable() {
        try {
            // Teste simples: tenta enviar uma mensagem de teste
            // Se RabbitMQ não estiver disponível, lançará exceção
            rabbitTemplate.execute(channel -> true);
            logger.info("AsyncProcessingService is available");
            return true;
        } catch (Exception e) {
            logger.error("AsyncProcessingService is not available", e);
            return false;
        }
    }
}
