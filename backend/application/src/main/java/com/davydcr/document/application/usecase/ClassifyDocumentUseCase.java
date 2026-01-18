package com.davydcr.document.application.usecase;

import com.davydcr.document.application.dto.ClassifyDocumentInput;
import com.davydcr.document.application.dto.ClassifyDocumentOutput;
import com.davydcr.document.application.port.ClassificationService;
import com.davydcr.document.application.port.DocumentRepository;
import com.davydcr.document.domain.exception.DomainException;
import com.davydcr.document.domain.model.Document;
import com.davydcr.document.domain.model.DocumentClassification;
import com.davydcr.document.domain.model.DocumentId;

import java.util.Objects;

/**
 * Use case para classificar um documento.
 * Reclassifica um documento já processado com novo texto.
 */
public class ClassifyDocumentUseCase {

    private final DocumentRepository documentRepository;
    private final ClassificationService classificationService;

    public ClassifyDocumentUseCase(DocumentRepository documentRepository,
                                    ClassificationService classificationService) {
        this.documentRepository = Objects.requireNonNull(documentRepository, "documentRepository cannot be null");
        this.classificationService = Objects.requireNonNull(classificationService, "classificationService cannot be null");
    }

    /**
     * Executa classificação de um documento com texto fornecido.
     */
    public ClassifyDocumentOutput execute(ClassifyDocumentInput input) {
        Objects.requireNonNull(input, "input cannot be null");

        try {
            if (input.getText().isBlank()) {
                throw new DomainException("Text cannot be blank for classification") {};
            }

            // Recuperar documento
            DocumentId docId = new DocumentId(java.util.UUID.fromString(input.getDocumentId()));
            Document document = documentRepository.findById(docId)
                    .orElseThrow(() -> new DomainException("Document not found: " + docId) {});

            // Executar classificação
            DocumentClassification classification = classificationService.classify(input.getText());

            // Retornar resultado
            return new ClassifyDocumentOutput(
                    input.getDocumentId(),
                    classification.getLabel().getValue(),
                    classification.getConfidence().getPercentage(),
                    classification.getModel()
            );

        } catch (DomainException e) {
            throw e;
        } catch (Exception e) {
            throw new DomainException("Error classifying document: " + e.getMessage()) {};
        }
    }
}
