package com.davydcr.document.infrastructure.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Serviço de Circuit Breaker para tarefas de processamento assíncrono.
 * 
 * Implementa padrão de circuit breaker para:
 * - Controlar taxa de falha
 * - Evitar cascata de falhas
 * - Monitoramento de saúde
 */
@Service
public class ProcessingCircuitBreakerService {

    private static final Logger log = LoggerFactory.getLogger(ProcessingCircuitBreakerService.class);

    @Value("${async.circuit-breaker.failure-threshold:50}")
    private int failureThresholdPercent;

    @Value("${async.circuit-breaker.window-size:100}")
    private int windowSize;

    // Estatísticas de falha por janela (Ring Buffer)
    private final Queue<Boolean> failureWindow = new LinkedList<>();
    private final AtomicInteger totalFailures = new AtomicInteger(0);
    private final AtomicInteger totalSuccesses = new AtomicInteger(0);

    /**
     * Registra sucesso.
     */
    public void recordSuccess() {
        synchronized (failureWindow) {
            failureWindow.offer(true);
            totalSuccesses.incrementAndGet();
            
            if (failureWindow.size() > windowSize) {
                failureWindow.poll();
            }
        }
        
        if (!isCircuitBreakerOpen() && totalFailures.get() > 0) {
            log.info("Circuit breaker recuperado - taxa de falha normal");
        }
    }

    /**
     * Registra falha.
     */
    public void recordFailure() {
        synchronized (failureWindow) {
            failureWindow.offer(false);
            totalFailures.incrementAndGet();
            
            if (failureWindow.size() > windowSize) {
                failureWindow.poll();
            }
            
            if (isCircuitBreakerOpen()) {
                log.warn("⚠️  CIRCUIT BREAKER ABERTO - Taxa de falha acima de {}%", failureThresholdPercent);
            }
        }
    }

    /**
     * Verifica se circuit breaker está aberto.
     */
    public boolean isCircuitBreakerOpen() {
        synchronized (failureWindow) {
            if (failureWindow.isEmpty() || failureWindow.size() < 10) {
                return false;
            }

            long failureCount = failureWindow.stream()
                .filter(success -> !success)
                .count();

            int failurePercent = (int) ((failureCount * 100) / failureWindow.size());
            return failurePercent >= failureThresholdPercent;
        }
    }

    /**
     * Obtém estatísticas do circuit breaker.
     */
    public Map<String, Object> getCircuitBreakerStatus() {
        synchronized (failureWindow) {
            long failureCount = failureWindow.stream()
                .filter(success -> !success)
                .count();

            int failurePercent = failureWindow.isEmpty() ? 0 : 
                (int) ((failureCount * 100) / failureWindow.size());

            Map<String, Object> status = new HashMap<>();
            status.put("isOpen", isCircuitBreakerOpen());
            status.put("failurePercentage", failurePercent);
            status.put("failureThreshold", failureThresholdPercent);
            status.put("windowSize", failureWindow.size());
            status.put("failureCount", failureCount);
            status.put("successCount", failureWindow.size() - failureCount);
            status.put("totalFailures", totalFailures.get());
            status.put("totalSuccesses", totalSuccesses.get());

            return status;
        }
    }

    /**
     * Reset manual do circuit breaker.
     */
    public void reset() {
        synchronized (failureWindow) {
            failureWindow.clear();
            totalFailures.set(0);
            totalSuccesses.set(0);
            log.info("Circuit breaker resetado manualmente");
        }
    }
}
