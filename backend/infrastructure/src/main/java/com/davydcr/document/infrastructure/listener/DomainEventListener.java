package com.davydcr.document.infrastructure.listener;

import com.davydcr.document.domain.event.DocumentProcessedEvent;
import com.davydcr.document.domain.event.DocumentStateChangedEvent;
import com.davydcr.document.domain.event.ProcessDocumentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * Listener para Domain Events publicados via RabbitMQ.
 * Recebe e registra eventos de documento.
 */
@Service
public class DomainEventListener {

    private static final Logger logger = LoggerFactory.getLogger(DomainEventListener.class);

    /**
     * Ouve eventos de mudança de estado de documento
     */
    @RabbitListener(queues = "document.state-changed.queue")
    public void handleDocumentStateChanged(DocumentStateChangedEvent event) {
        logger.info("Document state changed: documentId={}, {} -> {}, reason={}",
                event.documentId(),
                event.previousStatus(),
                event.newStatus(),
                event.reason());
    }

    /**
     * Ouve eventos de processamento iniciado
     * Nota: O processamento real é feito pelo ProcessDocumentEventHandler
     */
    @RabbitListener(queues = "document.processing.queue")
    public void handleProcessDocument(ProcessDocumentEvent event) {
        logger.info("Document processing requested: documentId={}, filePath={}",
                event.documentId(),
                event.filePath());
        // Nota: O ProcessDocumentEventHandler também ouve esta queue para fazer o processamento
    }

    /**
     * Ouve eventos de processamento completado
     */
    @RabbitListener(queues = "document.processed.queue")
    public void handleDocumentProcessed(DocumentProcessedEvent event) {
        if (event.success()) {
            logger.info("Document processed successfully: documentId={}, classification={}, confidence={}%",
                    event.documentId(),
                    event.classification(),
                    event.confidence());
        } else {
            logger.error("Document processing failed: documentId={}, error={}",
                    event.documentId(),
                    event.errorMessage());
        }
    }
}
