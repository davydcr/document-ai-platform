package com.davydcr.document.application.dto;

import com.davydcr.document.domain.model.DocumentId;
import java.util.Objects;

public class ExtractDocumentContentResult {

    private final DocumentId documentId;
    private final int pageCount;
    private final int textLength;
    private final String ocrEngine;

    public ExtractDocumentContentResult(DocumentId documentId, int pageCount, int textLength, String ocrEngine) {
        this.documentId = Objects.requireNonNull(documentId, "documentId cannot be null");
        this.pageCount = pageCount;
        this.textLength = textLength;
        this.ocrEngine = Objects.requireNonNull(ocrEngine, "ocrEngine cannot be null");

        if (pageCount < 0) {
            throw new IllegalArgumentException("pageCount cannot be negative");
        }
        if (textLength < 0) {
            throw new IllegalArgumentException("textLength cannot be negative");
        }
    }

    public DocumentId getDocumentId() {
        return documentId;
    }

    public int getPageCount() {
        return pageCount;
    }

    public int getTextLength() {
        return textLength;
    }

    public String getOcrEngine() {
        return ocrEngine;
    }

    @Override
    public String toString() {
        return "ExtractDocumentContentResult{" +
                "documentId=" + documentId +
                ", pageCount=" + pageCount +
                ", textLength=" + textLength +
                ", ocrEngine='" + ocrEngine + '\'' +
                '}';
    }
}
