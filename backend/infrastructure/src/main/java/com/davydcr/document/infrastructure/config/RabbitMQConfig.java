package com.davydcr.document.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuração de RabbitMQ para processamento assíncrono (Semana 7)
 * Define filas, exchanges e bindings para o fluxo de processamento de documentos
 */
@Configuration
@ConditionalOnProperty(
    name = "app.async.processing-enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class RabbitMQConfig {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQConfig.class);

    // Exchange names
    public static final String DOCUMENT_EXCHANGE = "document.exchange";
    public static final String DOCUMENT_EVENT_EXCHANGE = "document-exchange";

    // Queue names
    public static final String DOCUMENT_PROCESSING_QUEUE = "document.processing.queue";
    public static final String DOCUMENT_STATE_CHANGED_QUEUE = "document.state-changed.queue";
    public static final String DOCUMENT_PROCESSED_QUEUE = "document.processed.queue";
    public static final String WEBHOOK_EVENTS_QUEUE = "webhook-events-queue";

    // Routing keys
    public static final String DOCUMENT_PROCESS_ROUTING_KEY = "document.process";
    public static final String DOCUMENT_STATE_CHANGED_ROUTING_KEY = "document.state-changed";
    public static final String DOCUMENT_PROCESSED_ROUTING_KEY = "document.processed";

    // Message TTL (5 minutes)
    private static final int MESSAGE_TTL = 300000;

    /**
     * Define a fila principal de processamento de documentos
     */
    @Bean
    public Queue documentProcessingQueue() {
        logger.info("Creating document processing queue: {}", DOCUMENT_PROCESSING_QUEUE);
        return new Queue(
            DOCUMENT_PROCESSING_QUEUE,
            true,  // durable
            false, // exclusive
            false, // autoDelete
            new java.util.HashMap<String, Object>() {{
                put("x-message-ttl", MESSAGE_TTL);  // Messages expire after 5 minutes
                put("x-max-length", 10000);         // Maximum 10k messages in queue
            }}
        );
    }

    /**
     * Define o exchange direto para roteamento de mensagens
     */
    @Bean
    public DirectExchange documentExchange() {
        logger.info("Creating direct exchange: {}", DOCUMENT_EXCHANGE);
        return new DirectExchange(
            DOCUMENT_EXCHANGE,
            true,   // durable
            false   // autoDelete
        );
    }

    /**
     * Define o exchange direto para eventos de domínio
     */
    @Bean
    public DirectExchange documentEventExchange() {
        logger.info("Creating direct exchange: {}", DOCUMENT_EVENT_EXCHANGE);
        return new DirectExchange(
            DOCUMENT_EVENT_EXCHANGE,
            true,   // durable
            false   // autoDelete
        );
    }

    /**
     * Fila para eventos de mudança de estado
     */
    @Bean
    public Queue documentStateChangedQueue() {
        logger.info("Creating state changed queue: {}", DOCUMENT_STATE_CHANGED_QUEUE);
        return new Queue(
            DOCUMENT_STATE_CHANGED_QUEUE,
            true,  // durable
            false, // exclusive
            false  // autoDelete
        );
    }

    /**
     * Fila para eventos de processamento completado
     */
    @Bean
    public Queue documentProcessedQueue() {
        logger.info("Creating processed queue: {}", DOCUMENT_PROCESSED_QUEUE);
        return new Queue(
            DOCUMENT_PROCESSED_QUEUE,
            true,  // durable
            false, // exclusive
            false  // autoDelete
        );
    }

    /**
     * Fila para eventos de webhooks
     */
    @Bean
    public Queue webhookEventsQueue() {
        logger.info("Creating webhook events queue: {}", WEBHOOK_EVENTS_QUEUE);
        return new Queue(
            WEBHOOK_EVENTS_QUEUE,
            true,  // durable
            false, // exclusive
            false  // autoDelete
        );
    }

    /**
     * Binding entre a fila de processamento e o exchange
     */
    @Bean
    public Binding documentProcessingBinding(
            Queue documentProcessingQueue,
            DirectExchange documentExchange) {
        logger.info("Creating binding: {} -> {} (routing key: {})",
                DOCUMENT_PROCESSING_QUEUE, DOCUMENT_EXCHANGE, DOCUMENT_PROCESS_ROUTING_KEY);
        return BindingBuilder.bind(documentProcessingQueue)
                .to(documentExchange)
                .with(DOCUMENT_PROCESS_ROUTING_KEY);
    }

    /**
     * Binding para eventos de mudança de estado
     */
    @Bean
    public Binding documentStateChangedBinding(
            Queue documentStateChangedQueue,
            DirectExchange documentEventExchange) {
        logger.info("Creating binding: {} -> {} (routing key: {})",
                DOCUMENT_STATE_CHANGED_QUEUE, DOCUMENT_EVENT_EXCHANGE, DOCUMENT_STATE_CHANGED_ROUTING_KEY);
        return BindingBuilder.bind(documentStateChangedQueue)
                .to(documentEventExchange)
                .with(DOCUMENT_STATE_CHANGED_ROUTING_KEY);
    }

    /**
     * Binding para eventos de processamento completado
     */
    @Bean
    public Binding documentProcessedBinding(
            Queue documentProcessedQueue,
            DirectExchange documentEventExchange) {
        logger.info("Creating binding: {} -> {} (routing key: {})",
                DOCUMENT_PROCESSED_QUEUE, DOCUMENT_EVENT_EXCHANGE, DOCUMENT_PROCESSED_ROUTING_KEY);
        return BindingBuilder.bind(documentProcessedQueue)
                .to(documentEventExchange)
                .with(DOCUMENT_PROCESSED_ROUTING_KEY);
    }

    /**
     * Binding para eventos de processamento iniciado
     */
    @Bean
    public Binding documentProcessBinding(
            Queue documentProcessingQueue,
            DirectExchange documentEventExchange) {
        logger.info("Creating binding: {} -> {} (routing key: {})",
                DOCUMENT_PROCESSING_QUEUE, DOCUMENT_EVENT_EXCHANGE, DOCUMENT_PROCESS_ROUTING_KEY);
        return BindingBuilder.bind(documentProcessingQueue)
                .to(documentEventExchange)
                .with(DOCUMENT_PROCESS_ROUTING_KEY);
    }

    /**
     * Binding para eventos de webhooks
     */
    @Bean
    public Binding webhookEventsBinding(
            Queue webhookEventsQueue,
            DirectExchange documentEventExchange) {
        logger.info("Creating binding: {} -> {} (routing key: #)",
                WEBHOOK_EVENTS_QUEUE, DOCUMENT_EVENT_EXCHANGE);
        return BindingBuilder.bind(webhookEventsQueue)
                .to(documentEventExchange)
                .with("#");  // Catch all events
    }

    /**
     * Configura RabbitTemplate com converter JSON
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jackson2JsonMessageConverter());
        logger.info("RabbitTemplate configured with Jackson2JsonMessageConverter");
        return template;
    }

    /**
     * Converter para serializar/desserializar mensagens em JSON
     */
    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
