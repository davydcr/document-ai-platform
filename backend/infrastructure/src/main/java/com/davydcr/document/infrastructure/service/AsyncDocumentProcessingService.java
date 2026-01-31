package com.davydcr.document.infrastructure.service;

import com.davydcr.document.application.dto.ProcessDocumentInput;
import com.davydcr.document.application.dto.ProcessDocumentOutput;
import com.davydcr.document.application.port.DocumentRepository;
import com.davydcr.document.application.usecase.ProcessDocumentUseCase;
import com.davydcr.document.domain.model.DocumentId;
import com.davydcr.document.infrastructure.security.SecurityContextService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Serviço para processamento assíncrono de documentos.
 * 
 * Responsável por executar OCR e classificação em thread separada,
 * retornando CompletableFuture para não bloquear o cliente.
 * Notifica via webhook quando completa e rastreia saúde do sistema via circuit breaker.
 */
@Service
public class AsyncDocumentProcessingService {

    private static final Logger log = LoggerFactory.getLogger(AsyncDocumentProcessingService.class);

    private final ProcessDocumentUseCase processDocumentUseCase;
    private final DocumentRepository documentRepository;
    private final DocumentNotificationService notificationService;
    private final ProcessingCircuitBreakerService circuitBreakerService;

    @Autowired
    public AsyncDocumentProcessingService(
            ProcessDocumentUseCase processDocumentUseCase,
            DocumentRepository documentRepository,
            DocumentNotificationService notificationService,
            ProcessingCircuitBreakerService circuitBreakerService) {
        this.processDocumentUseCase = processDocumentUseCase;
        this.documentRepository = documentRepository;
        this.notificationService = notificationService;
        this.circuitBreakerService = circuitBreakerService;
    }

    /**
     * Processa documento de forma assíncrona em thread separada.
     * 
     * @param input Dados de entrada para processamento
     * @param fileName Nome do arquivo original
     * @param userId ID do usuário que fez o upload (propagado para a thread assíncrona)
     * @return CompletableFuture com resultado do processamento
     */
    @Async("documentProcessingExecutor")
    public CompletableFuture<ProcessDocumentOutput> processDocumentAsync(
            ProcessDocumentInput input,
            String fileName,
            String userId) {

        try {
            // Propagar userId para a thread assíncrona via ThreadLocal
            if (userId != null) {
                SecurityContextService.setAsyncUserId(userId);
                log.info("Iniciando processamento assíncrono de documento: {} para usuário: {}", fileName, userId);
            } else {
                log.info("Iniciando processamento assíncrono de documento: {} (usuário não identificado)", fileName);
            }
            
            // Executar processamento (OCR + classificação)
            ProcessDocumentOutput result = processDocumentUseCase
                .executeWithDocumentCreation(input, fileName);

            log.info("Processamento concluído com sucesso: documentId={}", result.getDocumentId());
            
            // Registrar sucesso no circuit breaker
            circuitBreakerService.recordSuccess();
            
            // Notificar via webhook
            try {
                notificationService.notifyCompletion(result.getDocumentId(), result);
            } catch (Exception e) {
                log.warn("Erro ao notificar webhook: {}", e.getMessage());
            }
            
            return CompletableFuture.completedFuture(result);
            
        } catch (Exception e) {
            log.error("Erro ao processar documento {}: {}", fileName, e.getMessage(), e);
            
            // Registrar falha no circuit breaker
            circuitBreakerService.recordFailure();
            
            // Notificar falha via webhook se documentId disponível
            if (input != null) {
                try {
                    notificationService.notifyFailure(input.getDocumentId(), e.getMessage());
                } catch (Exception notifyError) {
                    log.warn("Erro ao notificar falha: {}", notifyError.getMessage());
                }
            }
            
            return CompletableFuture.failedFuture(e);
        } finally {
            // Limpar ThreadLocal após processamento
            SecurityContextService.clearAsyncUserId();
        }
    }

    /**
     * Obtém o status atual de um documento.
     * 
     * @param documentId ID do documento
     * @return Status atual (PROCESSING, COMPLETED, FAILED, NOT_FOUND)
     */
    public String getDocumentProcessingStatus(String documentId) {
        try {
            // Usar UUID.fromString para converter string para DocumentId
            var docId = new DocumentId(java.util.UUID.fromString(documentId));
            var document = documentRepository.findById(docId);
            
            if (document.isEmpty()) {
                log.warn("Documento não encontrado: {}", documentId);
                return "NOT_FOUND";
            }
            
            String status = document.get().getStatus().name();  // Use .name() em vez de .value()
            log.debug("Status de documento {}: {}", documentId, status);
            return status;
            
        } catch (Exception e) {
            log.error("Erro ao obter status do documento {}: {}", documentId, e.getMessage());
            return "ERROR";
        }
    }

    /**
     * Registra webhook para notificação quando documento completa.
     * 
     * @param documentId ID do documento
     * @param webhookUrl URL para enviar notificação
     */
    public void registerWebhook(String documentId, String webhookUrl) {
        notificationService.registerWebhook(documentId, webhookUrl);
    }

    /**
     * Remove webhook registrado.
     * 
     * @param documentId ID do documento
     */
    public void unregisterWebhook(String documentId) {
        notificationService.unregisterWebhook(documentId);
    }
}
