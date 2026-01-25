package com.davydcr.document.infrastructure.config;

import com.davydcr.document.application.port.EventPublisher;
import com.davydcr.document.domain.event.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Implementação de EventPublisher usando RabbitMQ.
 * Mapeia tipos de eventos para routing keys e publica em document-exchange.
 */
@Service
public class RabbitEventPublisher implements EventPublisher {

    private static final String EXCHANGE_NAME = "document-exchange";

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public RabbitEventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = Objects.requireNonNull(rabbitTemplate, "rabbitTemplate cannot be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper cannot be null");
    }

    @Override
    public void publish(DomainEvent event) {
        Objects.requireNonNull(event, "event cannot be null");

        String routingKey = getRoutingKey(event);
        String message = convertToJson(event);

        rabbitTemplate.convertAndSend(EXCHANGE_NAME, routingKey, message);
    }

    private String getRoutingKey(DomainEvent event) {
        if (event instanceof DocumentStateChangedEvent) {
            return "document.state-changed";
        } else if (event instanceof ProcessDocumentEvent) {
            return "document.process";
        } else if (event instanceof DocumentProcessedEvent) {
            return "document.processed";
        } else {
            throw new IllegalArgumentException("Unknown event type: " + event.getClass().getSimpleName());
        }
    }

    private String convertToJson(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new RuntimeException("Error serializing event: " + e.getMessage(), e);
        }
    }
}
