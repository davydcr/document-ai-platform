package com.davydcr.document.application.dto;

import com.davydcr.document.domain.model.DocumentId;
import java.util.Objects;

public class ExtractDocumentContentCommand {

    private final DocumentId documentId;
    private final String documentPath;
    private final String ocrEngine;

    public ExtractDocumentContentCommand(DocumentId documentId, String documentPath, String ocrEngine) {
        this.documentId = Objects.requireNonNull(documentId, "documentId cannot be null");
        this.documentPath = Objects.requireNonNull(documentPath, "documentPath cannot be null");
        this.ocrEngine = Objects.requireNonNull(ocrEngine, "ocrEngine cannot be null");

        if (documentPath.isBlank()) {
            throw new IllegalArgumentException("documentPath cannot be blank");
        }
        if (ocrEngine.isBlank()) {
            throw new IllegalArgumentException("ocrEngine cannot be blank");
        }
    }

    public DocumentId getDocumentId() {
        return documentId;
    }

    public String getDocumentPath() {
        return documentPath;
    }

    public String getOcrEngine() {
        return ocrEngine;
    }
}
