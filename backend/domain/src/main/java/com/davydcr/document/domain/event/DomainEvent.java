package com.davydcr.document.domain.event;

import java.time.Instant;

/**
 * Marker interface para Domain Events.
 * Todos os eventos de domínio devem implementar esta interface.
 */
public interface DomainEvent {
    
    /**
     * @return momento em que o evento ocorreu
     */
    Instant occurredAt();
}
