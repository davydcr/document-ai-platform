package com.davydcr.document.application.usecase;

import com.davydcr.document.application.dto.GetDocumentOutput;
import com.davydcr.document.application.port.DocumentRepository;
import com.davydcr.document.domain.exception.DomainException;
import com.davydcr.document.domain.model.Document;
import com.davydcr.document.domain.model.DocumentId;

import java.util.Objects;

/**
 * Use case para recuperar informações de um documento.
 */
public class GetDocumentUseCase {

    private final DocumentRepository documentRepository;

    public GetDocumentUseCase(DocumentRepository documentRepository) {
        this.documentRepository = Objects.requireNonNull(documentRepository, "documentRepository cannot be null");
    }

    /**
     * Recupera documento pelo ID.
     */
    public GetDocumentOutput execute(String documentId) {
        Objects.requireNonNull(documentId, "documentId cannot be null");

        try {
            DocumentId docId = new DocumentId(java.util.UUID.fromString(documentId));
            Document document = documentRepository.findById(docId)
                    .orElseThrow(() -> new DomainException("Document not found: " + docId) {});

            int processedCount = (int) document.getProcessingHistory().stream()
                    .filter(pr -> pr.isSuccess())
                    .count();

            return new GetDocumentOutput(
                    document.getId().value().toString(),
                    document.getOriginalName(),
                    document.getType().name(),
                    document.getStatus().name(),
                    document.getCreatedAt().toString(),
                    document.getProcessingHistory().size(),
                    processedCount
            );

        } catch (DomainException e) {
            throw e;
        } catch (Exception e) {
            throw new DomainException("Error retrieving document: " + e.getMessage()) {};
        }
    }
}
