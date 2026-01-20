package com.davydcr.document.infrastructure.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serviço de observabilidade com métricas customizadas
 * Rastreia operações importantes da plataforma
 */
@Service
public class ObservabilityService {

    private static final Logger logger = LoggerFactory.getLogger(ObservabilityService.class);
    private final MeterRegistry meterRegistry;
    private final Map<String, Timer.Sample> timerSamples = new ConcurrentHashMap<>();

    public ObservabilityService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        initializeCustomMetrics();
    }

    /**
     * Inicializa métricas customizadas da aplicação
     */
    private void initializeCustomMetrics() {
        // Contadores para operações principais
        meterRegistry.counter("document.uploads.total");
        meterRegistry.counter("document.uploads.success");
        meterRegistry.counter("document.uploads.failed");

        meterRegistry.counter("document.ocr.total");
        meterRegistry.counter("document.ocr.success");
        meterRegistry.counter("document.ocr.failed");

        meterRegistry.counter("document.classification.total");
        meterRegistry.counter("document.classification.success");
        meterRegistry.counter("document.classification.failed");

        // Timers para medir latência
        meterRegistry.timer("document.upload.duration");
        meterRegistry.timer("document.ocr.duration");
        meterRegistry.timer("document.classification.duration");

        // Gauges para estado atual
        meterRegistry.gauge("document.processing.queue.size", 0);

        logger.info("Custom metrics initialized");
    }

    /**
     * Registra operação de upload bem-sucedida
     */
    public void recordUploadSuccess(long durationMs) {
        meterRegistry.counter("document.uploads.total").increment();
        meterRegistry.counter("document.uploads.success").increment();
        meterRegistry.timer("document.upload.duration").record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        logger.info("Upload completed successfully in {}ms", durationMs);
    }

    /**
     * Registra falha de upload
     */
    public void recordUploadFailure(String reason) {
        meterRegistry.counter("document.uploads.total").increment();
        meterRegistry.counter("document.uploads.failed").increment();
        logger.error("Upload failed: {}", reason);
    }

    /**
     * Registra operação de OCR bem-sucedida
     */
    public void recordOcrSuccess(long durationMs, int pageCount) {
        meterRegistry.counter("document.ocr.total").increment();
        meterRegistry.counter("document.ocr.success").increment();
        meterRegistry.timer("document.ocr.duration").record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        logger.info("OCR completed in {}ms for {} pages", durationMs, pageCount);
    }

    /**
     * Registra falha de OCR
     */
    public void recordOcrFailure(String reason) {
        meterRegistry.counter("document.ocr.total").increment();
        meterRegistry.counter("document.ocr.failed").increment();
        logger.error("OCR failed: {}", reason);
    }

    /**
     * Registra operação de classificação bem-sucedida
     */
    public void recordClassificationSuccess(long durationMs, String label, double confidence) {
        meterRegistry.counter("document.classification.total").increment();
        meterRegistry.counter("document.classification.success").increment();
        meterRegistry.timer("document.classification.duration").record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        logger.info("Classification completed in {}ms: label={}, confidence={}", durationMs, label, confidence);
    }

    /**
     * Registra falha de classificação
     */
    public void recordClassificationFailure(String reason) {
        meterRegistry.counter("document.classification.total").increment();
        meterRegistry.counter("document.classification.failed").increment();
        logger.error("Classification failed: {}", reason);
    }

    /**
     * Inicia timer para operação
     */
    public void startTimer(String operationName) {
        Timer.Sample sample = Timer.start(meterRegistry);
        timerSamples.put(operationName, sample);
        logger.debug("Timer started for operation: {}", operationName);
    }

    /**
     * Para timer e registra duração
     */
    public long stopTimer(String operationName, String timerName) {
        Timer.Sample sample = timerSamples.remove(operationName);
        if (sample != null) {
            long duration = sample.stop(meterRegistry.timer(timerName)) / 1_000_000; // Convert to ms
            logger.debug("Timer stopped for operation: {} - Duration: {}ms", operationName, duration);
            return duration;
        }
        logger.warn("No timer found for operation: {}", operationName);
        return 0;
    }

    /**
     * Log estruturado com contexto
     */
    public void logStructured(String operation, String status, Map<String, Object> context) {
        logger.info("Operation: {}, Status: {}, Context: {}", operation, status, context);
    }
}
