package com.davydcr.document.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuração de WebSocket com STOMP para updates em tempo real.
 * Semana 3 - Task 6
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(Runtime.getRuntime().availableProcessors());
        scheduler.setThreadNamePrefix("ws-scheduler-");
        scheduler.initialize();
        return scheduler;
    }

    /**
     * Configura o endpoint para conexão WebSocket.
     * Clients se conectam em: ws://localhost:8080/ws
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("*")
                .withSockJS();  // Fallback para browsers sem suporte WebSocket nativo
    }

    /**
     * Configura o message broker para pub/sub.
     * Destinos com prefixo "/topic" são broadcasts
     * Destinos com prefixo "/queue" são point-to-point
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Habilita in-memory message broker com prefixos de destino
        registry.enableSimpleBroker("/topic", "/queue")
                .setTaskScheduler(taskScheduler())
                .setHeartbeatValue(new long[]{25000, 25000});  // Heartbeat a cada 25s

        // Prefixo para mensagens enviadas pelo cliente para o servidor (handlers)
        registry.setApplicationDestinationPrefixes("/app");

        // Prefixo para mensagens de usuário específico (point-to-point)
        registry.setUserDestinationPrefix("/user");
    }
}
