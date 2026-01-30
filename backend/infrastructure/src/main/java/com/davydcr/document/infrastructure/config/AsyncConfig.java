package com.davydcr.document.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuração para processamento assíncrono de documentos.
 * 
 * Pool de threads dedicado para OCR e classificação, permitindo que
 * o upload retorne imediatamente enquanto o processamento acontece em background.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Executor para processamento de documentos.
     * 
     * - Core Threads: 4 (sempre ativas)
     * - Max Threads: 16 (escalável)
     * - Queue: 200 tasks
     * - Keep Alive: 60 segundos
     * - Rejeita tasks quando queue está cheio
     */
    @Bean(name = "documentProcessingExecutor")
    public Executor documentProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Thread pool configuration
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("document-processor-");
        
        // Rejection policy: CallerRunsPolicy (executa na thread do chamador se queue cheio)
        executor.setRejectedExecutionHandler(
            new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        executor.initialize();
        return executor;
    }

    /**
     * Executor para tarefas leves (notificações, webhooks).
     * 
     * - Core Threads: 2
     * - Max Threads: 8
     * - Queue: 100 tasks
     */
    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("notification-");
        executor.setRejectedExecutionHandler(
            new java.util.concurrent.ThreadPoolExecutor.DiscardPolicy()
        );
        
        executor.initialize();
        return executor;
    }
}
