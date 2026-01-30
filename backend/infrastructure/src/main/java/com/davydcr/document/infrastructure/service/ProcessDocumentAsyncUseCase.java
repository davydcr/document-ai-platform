package com.davydcr.document.infrastructure.service;

import com.davydcr.document.application.dto.ProcessDocumentInput;
import com.davydcr.document.application.dto.ProcessDocumentOutput;
import com.davydcr.document.application.usecase.ProcessDocumentUseCase;
import com.davydcr.document.domain.exception.DomainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Use case para processamento assíncrono de documentos.
 * 
 * Versão não-bloqueante que retorna CompletableFuture.
 */
@Component
public class ProcessDocumentAsyncUseCase {

    private static final Logger logger = LoggerFactory.getLogger(ProcessDocumentAsyncUseCase.class);

    private final ProcessDocumentUseCase processDocumentUseCase;

    @Autowired
    public ProcessDocumentAsyncUseCase(ProcessDocumentUseCase processDocumentUseCase) {
        this.processDocumentUseCase = processDocumentUseCase;
    }

    /**
     * Processa documento de forma assíncrona.
     * 
     * @param input Entrada do processamento
     * @param originalFileName Nome do arquivo original
     * @return CompletableFuture com resultado
     */
    @Async("documentProcessingExecutor")
    public CompletableFuture<ProcessDocumentOutput> executeAsync(
            ProcessDocumentInput input,
            String originalFileName) {

        try {
            logger.debug("Iniciando processamento assíncrono: {}", originalFileName);
            
            // Executar processamento síncrono em thread do pool
            ProcessDocumentOutput result = processDocumentUseCase
                .executeWithDocumentCreation(input, originalFileName);

            logger.info("Processamento assíncrono concluído: documentId={}, status={}", 
                result.getDocumentId(), result.getStatus());
            
            return CompletableFuture.completedFuture(result);
            
        } catch (DomainException e) {
            logger.error("Erro de domínio no processamento assíncrono: {}", e.getMessage());
            return CompletableFuture.failedFuture(e);
            
        } catch (Exception e) {
            logger.error("Erro inesperado no processamento assíncrono: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Processa com timeout e callback.
     * 
     * @param input Entrada do processamento
     * @param originalFileName Nome do arquivo original
     * @param timeoutMs Timeout em milissegundos
     * @param onSuccess Callback de sucesso
     * @param onError Callback de erro
     */
    @Async("documentProcessingExecutor")
    public void executeWithTimeout(
            ProcessDocumentInput input,
            String originalFileName,
            long timeoutMs,
            ProcessingCallback onSuccess,
            ErrorCallback onError) {

        long startTime = System.currentTimeMillis();
        
        try {
            logger.debug("Iniciando processamento com timeout de {}ms: {}", timeoutMs, originalFileName);
            
            ProcessDocumentOutput result = processDocumentUseCase
                .executeWithDocumentCreation(input, originalFileName);

            long elapsed = System.currentTimeMillis() - startTime;
            
            if (elapsed > timeoutMs) {
                logger.warn("Processamento excedeu timeout: esperado={}ms, real={}ms", timeoutMs, elapsed);
            }

            logger.info("Processamento com timeout concluído em {}ms", elapsed);
            
            if (onSuccess != null) {
                onSuccess.onSuccess(result);
            }
            
        } catch (Exception e) {
            logger.error("Erro durante processamento com timeout: {}", e.getMessage(), e);
            
            if (onError != null) {
                onError.onError(e);
            }
        }
    }

    /**
     * Callback para sucesso.
     */
    @FunctionalInterface
    public interface ProcessingCallback {
        void onSuccess(ProcessDocumentOutput result);
    }

    /**
     * Callback para erro.
     */
    @FunctionalInterface
    public interface ErrorCallback {
        void onError(Exception error);
    }
}
