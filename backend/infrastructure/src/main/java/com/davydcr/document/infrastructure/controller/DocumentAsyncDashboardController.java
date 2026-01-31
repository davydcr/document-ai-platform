package com.davydcr.document.infrastructure.controller;

import com.davydcr.document.infrastructure.service.ProcessingCircuitBreakerService;
import com.davydcr.document.infrastructure.service.DocumentNotificationService;
import com.davydcr.document.infrastructure.observability.ObservabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller para dashboard de monitoramento de processamento assíncrono.
 * 
 * Fornece métricas e status da fila de processamento.
 */
@RestController
@RequestMapping("/api/documents/async/dashboard")
@CrossOrigin(origins = "*")
@Tag(name = "Async Dashboard", description = "Dashboard de monitoramento do processamento assíncrono")
public class DocumentAsyncDashboardController {

    private final ProcessingCircuitBreakerService circuitBreakerService;
    private final DocumentNotificationService notificationService;
    private final ObservabilityService observabilityService;

    @Autowired
    public DocumentAsyncDashboardController(
            ProcessingCircuitBreakerService circuitBreakerService,
            DocumentNotificationService notificationService,
            ObservabilityService observabilityService) {
        this.circuitBreakerService = circuitBreakerService;
        this.notificationService = notificationService;
        this.observabilityService = observabilityService;
    }

    @GetMapping("/metrics")
    @Operation(summary = "Obter métricas de processamento",
        description = "Retorna métricas sobre uploads, OCR, classificação e falhas")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Métricas retornadas com sucesso")
    })
    public ResponseEntity<Map<String, Object>> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // Adicionar métricas do circuit breaker
        metrics.put("circuitBreaker", circuitBreakerService.getCircuitBreakerStatus());

        // Adicionar informações de webhooks
        metrics.put("webhooks", Map.of(
            "activeSubscriptions", notificationService.getActiveSubscriptionCount()
        ));

        // Adicionar timestamp
        metrics.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/circuit-breaker/status")
    @Operation(summary = "Status do circuit breaker",
        description = "Retorna status detalhado do circuit breaker de processamento")
    public ResponseEntity<Map<String, Object>> getCircuitBreakerStatus() {
        return ResponseEntity.ok(circuitBreakerService.getCircuitBreakerStatus());
    }

    @PostMapping("/circuit-breaker/reset")
    @Operation(summary = "Reset do circuit breaker",
        description = "Reseta manualmente o circuit breaker")
    public ResponseEntity<Map<String, String>> resetCircuitBreaker() {
        circuitBreakerService.reset();
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Circuit breaker resetado com sucesso");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    @Operation(summary = "Health check do serviço assíncrono",
        description = "Verifica saúde do sistema de processamento assíncrono")
    public ResponseEntity<Map<String, Object>> getAsyncHealth() {
        Map<String, Object> health = new HashMap<>();

        // Status do circuit breaker
        Map<String, Object> cbStatus = circuitBreakerService.getCircuitBreakerStatus();
        boolean cbHealthy = !(boolean) cbStatus.get("isOpen");

        // Contar webhooks ativos
        int activeWebhooks = notificationService.getActiveSubscriptionCount();

        // Status geral
        boolean isHealthy = cbHealthy;

        health.put("status", isHealthy ? "UP" : "DEGRADED");
        health.put("circuitBreakerHealthy", cbHealthy);
        health.put("activeWebhooks", activeWebhooks);
        health.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(health);
    }

    @GetMapping("/queue")
    @Operation(summary = "Status da fila de processamento",
        description = "Retorna informações sobre a fila de processamento (documentos em processamento)")
    public ResponseEntity<Map<String, Object>> getQueueStatus() {
        Map<String, Object> queueInfo = new HashMap<>();

        // Informações do circuit breaker que incluem ativas tentativas
        Map<String, Object> cbStatus = circuitBreakerService.getCircuitBreakerStatus();
        int activeRetries = (int) cbStatus.get("activeRetries");

        // Status geral
        queueInfo.put("activeRetries", activeRetries);
        queueInfo.put("activeWebhooks", notificationService.getActiveSubscriptionCount());
        queueInfo.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(queueInfo);
    }
}
