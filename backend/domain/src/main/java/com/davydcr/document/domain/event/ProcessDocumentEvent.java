package com.davydcr.document.domain.event;

import java.time.Instant;

/**
 * Evento disparado quando um documento solicita processamento.
 */
public record ProcessDocumentEvent(
        String documentId,
        String filePath,
        Instant occurredAt
) implements DomainEvent {

    public ProcessDocumentEvent {
        if (documentId == null || documentId.isBlank()) {
            throw new IllegalArgumentException("documentId cannot be null or blank");
        }
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("filePath cannot be null or blank");
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }
}
