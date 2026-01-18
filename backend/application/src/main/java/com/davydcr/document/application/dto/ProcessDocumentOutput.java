package com.davydcr.document.application.dto;

import java.util.Objects;

/**
 * DTO para saída do caso de uso de processar documento.
 */
public class ProcessDocumentOutput {

    private final String documentId;
    private final String status;
    private final String extractedTextPreview;
    private final String classification;
    private final Integer confidencePercentage;

    public ProcessDocumentOutput(String documentId, String status, String extractedTextPreview,
                                 String classification, Integer confidencePercentage) {
        this.documentId = Objects.requireNonNull(documentId, "documentId cannot be null");
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.extractedTextPreview = extractedTextPreview;
        this.classification = classification;
        this.confidencePercentage = confidencePercentage;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getStatus() {
        return status;
    }

    public String getExtractedTextPreview() {
        return extractedTextPreview;
    }

    public String getClassification() {
        return classification;
    }

    public Integer getConfidencePercentage() {
        return confidencePercentage;
    }

    @Override
    public String toString() {
        return "ProcessDocumentOutput{" +
                "documentId='" + documentId + '\'' +
                ", status='" + status + '\'' +
                ", classification='" + classification + '\'' +
                ", confidence=" + confidencePercentage + "%" +
                '}';
    }
}
