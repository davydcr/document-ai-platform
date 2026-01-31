package com.davydcr.document.infrastructure.controller;

import com.davydcr.document.application.dto.ProcessDocumentInput;
import com.davydcr.document.application.dto.ProcessDocumentOutput;
import com.davydcr.document.infrastructure.service.AsyncDocumentProcessingService;
import com.davydcr.document.infrastructure.service.DocumentStorageService;
import com.davydcr.document.infrastructure.observability.ObservabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Controller para operações assíncronas de documentos.
 * 
 * Upload retorna 202 Accepted com documentId,
 * cliente usa GET /documents/async/{id}/status para verificar progresso.
 */
@RestController
@RequestMapping("/api/documents/async")
@CrossOrigin(origins = "*")
@Tag(name = "Async Documents", description = "API assíncrona para processamento de documentos")
public class DocumentAsyncController {

    private static final Logger log = LoggerFactory.getLogger(DocumentAsyncController.class);
    private static final long DEFAULT_POLLING_TIMEOUT_MS = 30000; // 30 segundos

    private final AsyncDocumentProcessingService asyncProcessingService;
    private final DocumentStorageService storageService;
    private final ObservabilityService observabilityService;

    @Autowired
    public DocumentAsyncController(
            AsyncDocumentProcessingService asyncProcessingService,
            DocumentStorageService storageService,
            ObservabilityService observabilityService) {
        this.asyncProcessingService = asyncProcessingService;
        this.storageService = storageService;
        this.observabilityService = observabilityService;
    }

    @PostMapping("/upload")
    @Operation(summary = "Upload assíncrono de documento",
        description = "Realiza upload de documento e retorna imediatamente com status PROCESSING. " +
            "O processamento (OCR + classificação) ocorre em background.",
        tags = {"Async Documents"})
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Documento aceito para processamento",
            content = @Content(schema = @Schema(implementation = ProcessDocumentOutput.class))),
        @ApiResponse(responseCode = "400", description = "Tipo de arquivo inválido ou arquivo vazio"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<ProcessDocumentOutput> uploadDocumentAsync(
            @Parameter(description = "Arquivo do documento", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Tipo do documento (PDF, IMAGE, TXT)", example = "PDF")
            @RequestParam(defaultValue = "PDF") String fileType,
            @Parameter(description = "Timeout customizável em ms (padrão: 60000)")
            @RequestParam(defaultValue = "60000") long timeoutMs,
            Authentication authentication) {
        
        long startTime = System.currentTimeMillis();
        String userId = authentication != null ? authentication.getName() : "anonymous";

        try {
            // Validações básicas
            if (file == null || file.isEmpty()) {
                log.warn("Upload attempt with empty file");
                return ResponseEntity.badRequest().build();
            }

            String filename = file.getOriginalFilename() != null 
                ? file.getOriginalFilename() 
                : "document";

            log.info("Async upload iniciado: file={}, type={}, user={}, timeout={}ms", 
                filename, fileType, userId, timeoutMs);

            // Gerar document ID
            String documentId = UUID.randomUUID().toString();

            // Salvar arquivo
            String filePath = storageService.saveDocument(file);
            log.info("Document saved to: {} with ID: {}", filePath, documentId);

            // Preparar input com timeout
            ProcessDocumentInput input = new ProcessDocumentInput(
                documentId,
                filePath,
                fileType
            );

            // Iniciar processamento assíncrono em background
            asyncProcessingService.processDocumentAsync(input, filename)
                .thenAccept(result -> {
                    long elapsed = System.currentTimeMillis() - startTime;
                    observabilityService.recordUploadSuccess(elapsed);
                    log.info("Async processing completed: {} in {}ms", documentId, elapsed);
                })
                .exceptionally(e -> {
                    long elapsed = System.currentTimeMillis() - startTime;
                    observabilityService.recordUploadFailure(e.getMessage());
                    log.error("Async processing failed: {}", e.getMessage(), e);
                    return null;
                });

            // Retornar 202 Accepted imediatamente
            ProcessDocumentOutput response = new ProcessDocumentOutput(
                documentId,
                "PROCESSING",
                null,
                null,
                null
            );

            log.info("Async upload accepted: {} (202)", documentId);
            return ResponseEntity.accepted().body(response);

        } catch (Exception e) {
            log.error("Erro no upload assíncrono: {}", e.getMessage(), e);
            observabilityService.recordUploadFailure(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{documentId}/status")
    @Operation(summary = "Verificar status de processamento",
        description = "Retorna status atual do documento (PROCESSING, COMPLETED, FAILED)",
        tags = {"Async Documents"})
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status retornado",
            content = @Content(schema = @Schema(
                example = "{\"documentId\": \"...\", \"status\": \"PROCESSING\", \"timestamp\": 1234567890}"
            ))),
        @ApiResponse(responseCode = "404", description = "Documento não encontrado"),
        @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<DocumentStatusResponse> getDocumentStatus(
            @PathVariable String documentId) {

        try {
            // Obter status atual sem aguardar
            String status = asyncProcessingService.getDocumentProcessingStatus(documentId);
            
            DocumentStatusResponse response = new DocumentStatusResponse(
                documentId,
                status,
                System.currentTimeMillis()
            );

            log.debug("Status check for document {}: {}", documentId, status);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erro ao obter status do documento: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{documentId}/status/polling")
    @Operation(summary = "Polling com timeout",
        description = "Aguarda até N segundos por completion do documento. Retorna imediatamente quando completo.",
        tags = {"Async Documents"})
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status final ou timeout atingido"),
        @ApiResponse(responseCode = "404", description = "Documento não encontrado"),
        @ApiResponse(responseCode = "408", description = "Polling interrompido")
    })
    public ResponseEntity<DocumentStatusResponse> pollDocumentStatus(
            @PathVariable String documentId,
            @Parameter(description = "Timeout de polling em millisegundos (padrão: 30000)")
            @RequestParam(defaultValue = "30000") long timeoutMs) {

        long startTime = System.currentTimeMillis();
        
        try {
            log.debug("Starting polling for document {} with timeout {}ms", documentId, timeoutMs);

            // Polling com timeout
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                String status = asyncProcessingService.getDocumentProcessingStatus(documentId);

                // Se documento foi completado ou falhou, retornar imediatamente
                if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    log.info("Polling completed for document {}: {} ({}ms)", documentId, status, elapsedTime);
                    
                    DocumentStatusResponse response = new DocumentStatusResponse(
                        documentId,
                        status,
                        System.currentTimeMillis()
                    );
                    return ResponseEntity.ok(response);
                }

                // Aguardar 500ms antes de tentar novamente
                Thread.sleep(500);
            }

            // Timeout atingido, retornar status atual
            long elapsedTime = System.currentTimeMillis() - startTime;
            String status = asyncProcessingService.getDocumentProcessingStatus(documentId);
            log.warn("Polling timeout reached for document {}: {} ({}ms)", documentId, status, elapsedTime);
            
            DocumentStatusResponse response = new DocumentStatusResponse(
                documentId,
                status,
                System.currentTimeMillis()
            );

            return ResponseEntity.ok(response);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Polling interrompido para documento {}: {}", documentId, e.getMessage());
            return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).build();
            
        } catch (Exception e) {
            log.error("Erro durante polling do documento {}: {}", documentId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{documentId}/wait")
    @Operation(summary = "Aguardar completion (bloqueante)",
        description = "Aguarda até o documento ser processado, retorna resultado final. Útil para casos onde cliente quer esperar.",
        tags = {"Async Documents"})
    public ResponseEntity<DocumentStatusResponse> waitForCompletion(
            @PathVariable String documentId,
            @Parameter(description = "Timeout máximo em ms (padrão: 300000 = 5 min)")
            @RequestParam(defaultValue = "300000") long maxWaitMs) {
        
        // Delegar para polling com timeout maior
        return pollDocumentStatus(documentId, maxWaitMs);
    }

    @PostMapping("/{documentId}/webhook/register")
    @Operation(summary = "Registrar webhook para notificação",
        description = "Registra uma URL para receber notificação quando documento completar",
        tags = {"Async Documents"})
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Webhook registrado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Parâmetros inválidos")
    })
    public ResponseEntity<Map<String, String>> registerWebhook(
            @PathVariable String documentId,
            @RequestParam String webhookUrl) {

        if (documentId == null || webhookUrl == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            asyncProcessingService.registerWebhook(documentId, webhookUrl);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Webhook registrado com sucesso");
            response.put("documentId", documentId);
            response.put("webhookUrl", webhookUrl);
            
            log.info("Webhook registrado para documento {}: {}", documentId, webhookUrl);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erro ao registrar webhook: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{documentId}/webhook")
    @Operation(summary = "Remover webhook",
        description = "Remove webhook registrado para um documento",
        tags = {"Async Documents"})
    public ResponseEntity<Void> unregisterWebhook(@PathVariable String documentId) {
        try {
            asyncProcessingService.unregisterWebhook(documentId);
            log.info("Webhook removido para documento {}", documentId);
            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            log.error("Erro ao remover webhook: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * DTO para resposta de status.
     */
    public record DocumentStatusResponse(
        String documentId,
        String status,
        long timestamp
    ) {}
}
