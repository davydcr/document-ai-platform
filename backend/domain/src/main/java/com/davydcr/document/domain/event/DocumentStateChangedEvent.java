package com.davydcr.document.domain.event;

import com.davydcr.document.domain.model.DocumentStatus;
import java.time.Instant;

/**
 * Evento disparado quando o status do documento muda.
 */
public record DocumentStateChangedEvent(
        String documentId,
        DocumentStatus previousStatus,
        DocumentStatus newStatus,
        String reason,
        Instant occurredAt
) implements DomainEvent {

    public DocumentStateChangedEvent {
        if (documentId == null || documentId.isBlank()) {
            throw new IllegalArgumentException("documentId cannot be null or blank");
        }
        if (previousStatus == null || newStatus == null) {
            throw new IllegalArgumentException("status cannot be null");
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }
}
