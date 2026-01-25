package com.davydcr.document.application.port;

import com.davydcr.document.domain.event.DomainEvent;

/**
 * Port para publicação de Domain Events.
 * Implementações podem usar RabbitMQ, Kafka, Event Bus, etc.
 */
public interface EventPublisher {

    /**
     * Publica um evento de domínio.
     */
    void publish(DomainEvent event);
}
