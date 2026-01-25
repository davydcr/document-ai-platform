package com.davydcr.document.domain.event;

import java.time.Instant;

/**
 * Evento disparado quando um documento foi processado (com sucesso ou erro).
 */
public record DocumentProcessedEvent(
        String documentId,
        String extractedText,
        String classification,
        Integer confidence,
        Boolean success,
        String errorMessage,
        Instant occurredAt
) implements DomainEvent {

    public DocumentProcessedEvent {
        if (documentId == null || documentId.isBlank()) {
            throw new IllegalArgumentException("documentId cannot be null or blank");
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }

    /**
     * Factory method para criar um evento de falha.
     */
    public static DocumentProcessedEvent failure(String documentId, String errorMessage) {
        return new DocumentProcessedEvent(
                documentId,
                "",
                "",
                0,
                false,
                errorMessage,
                Instant.now()
        );
    }

    /**
     * Factory method para criar um evento de sucesso.
     */
    public static DocumentProcessedEvent success(
            String documentId,
            String extractedText,
            String classification,
            Integer confidence) {
        return new DocumentProcessedEvent(
                documentId,
                extractedText,
                classification,
                confidence,
                true,
                null,
                Instant.now()
        );
    }
}
