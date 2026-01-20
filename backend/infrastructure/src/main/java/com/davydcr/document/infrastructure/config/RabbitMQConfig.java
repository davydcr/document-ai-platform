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

    // Queue names
    public static final String DOCUMENT_PROCESSING_QUEUE = "document.processing.queue";

    // Routing keys
    public static final String DOCUMENT_PROCESS_ROUTING_KEY = "document.process";

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
