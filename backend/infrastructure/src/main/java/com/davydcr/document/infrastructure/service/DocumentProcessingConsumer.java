package com.davydcr.document.infrastructure.service;

import com.davydcr.document.application.dto.DocumentProcessingMessage;
import com.davydcr.document.application.usecase.ProcessDocumentUseCase;
import com.davydcr.document.infrastructure.config.RabbitMQConfig;
import com.davydcr.document.infrastructure.observability.ObservabilityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Consumer (listener) para processar mensagens da fila RabbitMQ
 * Implementa padrão de consumer para processamento assíncrono (Semana 7)
 */
@Service
@ConditionalOnProperty(
    name = "app.async.processing-enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class DocumentProcessingConsumer {

    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessingConsumer.class);

    private final ProcessDocumentUseCase processDocumentUseCase;
    private final ObservabilityService observabilityService;

    public DocumentProcessingConsumer(
            ProcessDocumentUseCase processDocumentUseCase,
            ObservabilityService observabilityService) {
        this.processDocumentUseCase = processDocumentUseCase;
        this.observabilityService = observabilityService;
        logger.info("DocumentProcessingConsumer initialized");
    }

    /**
     * Listener para mensagens na fila de processamento
     * Processa documentos de forma assíncrona
     */
    @RabbitListener(queues = RabbitMQConfig.DOCUMENT_PROCESSING_QUEUE)
    public void processDocument(DocumentProcessingMessage message) {
        long startTime = System.currentTimeMillis();
        
        logger.info("Processing document from queue: documentId={}, fileType={}", 
                message.getDocumentId(), message.getFileType());

        try {
            // Aqui entraríamos a lógica de processamento
            // Por enquanto, apenas log
            // Em uma implementação real, teríamos:
            // - Carregar arquivo do storage
            // - Executar OCR
            // - Fazer classificação
            // - Salvar resultados

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Document queued for processing: documentId={}, duration={}ms", 
                    message.getDocumentId(), duration);
            
            // Registrar métrica de sucesso
            observabilityService.recordOcrSuccess(duration, 1);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Error processing document: documentId={}, duration={}ms", 
                    message.getDocumentId(), duration, e);
            
            observabilityService.recordOcrFailure(e.getMessage());
            
            // Em produção, poderia reenviar para DLQ (Dead Letter Queue)
            // Por enquanto, apenas loga o erro
        }
    }
}
