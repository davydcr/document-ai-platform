package com.davydcr.document.infrastructure.worker;

import com.davydcr.document.application.port.ClassificationService;
import com.davydcr.document.application.port.DocumentRepository;
import com.davydcr.document.application.port.EventPublisher;
import com.davydcr.document.application.port.OcrService;
import com.davydcr.document.domain.event.ProcessDocumentEvent;
import com.davydcr.document.domain.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 * Worker assincronamente que processa documentos.
 * Ouve eventos de ProcessDocumentEvent e executa:
 * 1. OCR (extração de texto)
 * 2. Classificação
 * 3. Transição de estado
 * 4. Publicação de eventos de conclusão
 */
@Service
public class ProcessDocumentEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(ProcessDocumentEventHandler.class);

    private final DocumentRepository documentRepository;
    private final ClassificationService classificationService;
    private final EventPublisher eventPublisher;

    public ProcessDocumentEventHandler(
            DocumentRepository documentRepository,
            ClassificationService classificationService,
            EventPublisher eventPublisher) {
        this.documentRepository = Objects.requireNonNull(documentRepository);
        this.classificationService = Objects.requireNonNull(classificationService);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
    }

    /**
     * Ouve eventos de ProcessDocumentEvent e processa documentos.
     * Executa em background sem bloquear a requisição HTTP.
     */
    @RabbitListener(queues = "document.processing.queue")
    public void handleProcessDocumentEvent(ProcessDocumentEvent event) {
        Objects.requireNonNull(event, "event cannot be null");

        logger.info("Processing document: documentId={}, filePath={}",
                event.documentId(), event.filePath());

        try {
            // Recuperar documento do repositório
            DocumentId docId = new DocumentId(java.util.UUID.fromString(event.documentId()));
            Document document = documentRepository.findById(docId)
                    .orElseThrow(() -> new IllegalArgumentException("Document not found: " + docId));

            // Validar estado
            if (!document.getStatus().equals(DocumentStatus.PROCESSING)) {
                logger.warn("Document not in PROCESSING state: documentId={}, status={}",
                        event.documentId(), document.getStatus());
                return;
            }

            // Executar OCR
            logger.debug("Starting OCR extraction for: {}", event.filePath());
            ExtractedContent extractedContent = performOcr(event.filePath());

            // Executar Classificação
            logger.debug("Starting classification for: {}", event.documentId());
            DocumentClassification classification = classificationService.classify(extractedContent);

            // Criar resultado de processamento
            ProcessingResult result = new ProcessingResult(
                    ProcessingStatus.SUCCESS,
                    Map.of(
                            "extracted_pages", extractedContent.getPageCount(),
                            "ocr_engine", extractedContent.getOcrEngine(),
                            "text_length", extractedContent.getFullText().length()
                    ),
                    classificationService.getModelName(),
                    extractedContent,
                    classification
            );

            // Completar processamento no domínio
            document.completeProcessing(result);

            // Salvar documento atualizado
            documentRepository.save(document);

            // Publicar eventos de conclusão
            document.getDomainEvents().forEach(eventPublisher::publish);
            document.clearDomainEvents();

            logger.info("Document processed successfully: documentId={}, classification={}",
                    event.documentId(), classification.getLabel().getValue());

        } catch (Exception e) {
            logger.error("Error processing document: documentId={}, error={}",
                    event.documentId(), e.getMessage(), e);

            try {
                // Tentar recuperar documento e marcar como falho
                DocumentId docId = new DocumentId(java.util.UUID.fromString(event.documentId()));
                Document document = documentRepository.findById(docId).orElse(null);

                if (document != null && document.getStatus().equals(DocumentStatus.PROCESSING)) {
                    document.failProcessing(e.getMessage());
                    documentRepository.save(document);

                    // Publicar eventos de falha
                    document.getDomainEvents().forEach(eventPublisher::publish);
                    document.clearDomainEvents();
                }
            } catch (Exception ex) {
                logger.error("Error marking document as failed: {}", ex.getMessage());
            }
        }
    }

    /**
     * Executa OCR no arquivo do documento.
     */
    private ExtractedContent performOcr(String filePath) throws IOException {
        // Simular OCR - em produção seria integração com Tesseract/PaddleOCR
        // Por enquanto retorna conteúdo simulado
        return new ExtractedContent(
                "Document content extracted from: " + filePath,
                1,
                "Tesseract"
        );
    }
}
