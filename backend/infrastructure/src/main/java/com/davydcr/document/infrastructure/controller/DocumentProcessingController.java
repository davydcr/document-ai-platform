package com.davydcr.document.infrastructure.controller;

import com.davydcr.document.application.dto.ProcessDocumentInput;
import com.davydcr.document.application.dto.ProcessDocumentOutput;
import com.davydcr.document.application.usecase.ProcessDocumentUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Objects;
import java.util.UUID;

/**
 * REST API Controller para processamento de documentos.
 * Endpoint assincronamente que retorna 202 Accepted imediatamente.
 */
@RestController
@RequestMapping("/api/documents")
public class DocumentProcessingController {

    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessingController.class);

    private final ProcessDocumentUseCase processDocumentUseCase;

    public DocumentProcessingController(ProcessDocumentUseCase processDocumentUseCase) {
        this.processDocumentUseCase = Objects.requireNonNull(processDocumentUseCase);
    }

    /**
     * Inicia processamento assincronamente de um documento.
     * Retorna 202 Accepted imediatamente.
     * 
     * @param documentId ID do documento (UUID format)
     * @param request Request com filePath e fileType
     * @return 202 Accepted com Location header
     */
    @PostMapping("/{id}/process")
    public ResponseEntity<ProcessDocumentOutput> processDocument(
            @PathVariable("id") String documentId,
            @RequestBody ProcessDocumentRequest request) {

        logger.info("Processing document asynchronously: documentId={}, filePath={}",
                documentId, request.filePath);

        // Validar UUID
        if (!isValidUUID(documentId)) {
            logger.warn("Invalid document ID format: {}", documentId);
            return ResponseEntity.badRequest().build();
        }

        // Validar request
        Objects.requireNonNull(request, "request cannot be null");

        // Chamar use case
        ProcessDocumentInput input = new ProcessDocumentInput(
                documentId,
                request.filePath(),
                request.fileType()
        );

        ProcessDocumentOutput output = processDocumentUseCase.execute(input);

        // Retornar 202 Accepted com Location header
        return ResponseEntity.accepted()
                .location(URI.create("/api/documents/" + documentId))
                .body(output);
    }

    /**
     * Valida se uma string é um UUID válido.
     */
    private boolean isValidUUID(String id) {
        try {
            UUID.fromString(id);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * DTO para requisição de processamento.
     */
    public record ProcessDocumentRequest(
            String filePath,
            String fileType
    ) {
    }
}
