package com.davydcr.document.domain.model;

import com.davydcr.document.domain.event.DomainEvent;
import com.davydcr.document.domain.event.DocumentProcessedEvent;
import com.davydcr.document.domain.event.DocumentStateChangedEvent;
import com.davydcr.document.domain.event.ProcessDocumentEvent;
import com.davydcr.document.domain.exception.DomainException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Document {

    private final DocumentId id;
    private final String originalName;
    private final DocumentType type;
    private DocumentStatus status;
    private final Instant createdAt;
    private String errorMessage;
    private final List<ProcessingResult> processingHistory = new ArrayList<>();
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    public Document(DocumentId id,
                    String originalName,
                    DocumentType type) {

        this.id = Objects.requireNonNull(id);
        this.originalName = Objects.requireNonNull(originalName);
        this.type = Objects.requireNonNull(type);
        this.status = DocumentStatus.RECEIVED;
        this.createdAt = Instant.now();
    }

    /**
     * Solicita processamento do documento.
     * Transiciona de RECEIVED para PROCESSING.
     * Publica eventos: DocumentStateChangedEvent + ProcessDocumentEvent
     */
    public void requestProcessing() {
        if (!status.canTransitionTo(DocumentStatus.PROCESSING)) {
            throw new DomainException(
                    "Cannot request processing for document in state: " + status
            ) {};
        }

        DocumentStatus previousStatus = this.status;
        this.status = DocumentStatus.PROCESSING;

        // Publica eventos
        publishEvent(new DocumentStateChangedEvent(
                id.value().toString(),
                previousStatus,
                DocumentStatus.PROCESSING,
                "Document processing requested",
                Instant.now()
        ));

        publishEvent(new ProcessDocumentEvent(
                id.value().toString(),
                originalName,
                Instant.now()
        ));
    }

    /**
     * Completa o processamento do documento com sucesso.
     * Transiciona de PROCESSING para COMPLETED.
     * Publica eventos: DocumentStateChangedEvent + DocumentProcessedEvent
     */
    public void completeProcessing(ProcessingResult result) {
        if (!status.canTransitionTo(DocumentStatus.COMPLETED)) {
            throw new DomainException(
                    "Cannot complete processing for document in state: " + status
            ) {};
        }

        this.processingHistory.add(result);
        this.status = DocumentStatus.COMPLETED;

        // Extrai dados do resultado
        String extractedText = "";
        String classification = "";
        Integer confidence = 0;

        if (result.getExtractedContent().isPresent()) {
            extractedText = result.getExtractedContent().get().getFullText();
        }

        if (result.getClassification().isPresent()) {
            DocumentClassification classif = result.getClassification().get();
            classification = classif.getLabel().getValue();
            confidence = classif.getConfidence().getPercentage();
        }

        // Publica eventos
        publishEvent(new DocumentStateChangedEvent(
                id.value().toString(),
                DocumentStatus.PROCESSING,
                DocumentStatus.COMPLETED,
                "Document processing completed",
                Instant.now()
        ));

        publishEvent(DocumentProcessedEvent.success(
                id.value().toString(),
                extractedText,
                classification,
                confidence
        ));
    }

    /**
     * Falha no processamento do documento.
     * Transiciona de PROCESSING para FAILED.
     * Publica eventos: DocumentStateChangedEvent + DocumentProcessedEvent (failure)
     */
    public void failProcessing(String error) {
        if (!status.canTransitionTo(DocumentStatus.FAILED)) {
            throw new DomainException(
                    "Cannot fail processing for document in state: " + status
            ) {};
        }

        this.status = DocumentStatus.FAILED;
        this.errorMessage = error;

        // Publica eventos
        publishEvent(new DocumentStateChangedEvent(
                id.value().toString(),
                DocumentStatus.PROCESSING,
                DocumentStatus.FAILED,
                "Document processing failed: " + error,
                Instant.now()
        ));

        publishEvent(DocumentProcessedEvent.failure(
                id.value().toString(),
                error
        ));
    }

    /**
     * Publica um evento de domínio.
     */
    private void publishEvent(DomainEvent event) {
        domainEvents.add(event);
    }

    /**
     * Retorna todos os eventos não publicados.
     */
    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    /**
     * Limpa a lista de eventos após publicação.
     */
    public void clearDomainEvents() {
        domainEvents.clear();
    }

    public List<ProcessingResult> getProcessingHistory() {
        return Collections.unmodifiableList(processingHistory);
    }

    // getters
    public DocumentId getId() {
        return id;
    }

    public String getOriginalName() {
        return originalName;
    }

    public DocumentType getType() {
        return type;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
