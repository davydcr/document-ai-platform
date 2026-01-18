package com.davydcr.document.application.dto;

import java.util.Objects;

/**
 * DTO para saída do caso de uso de classificar documento.
 */
public class ClassifyDocumentOutput {

    private final String documentId;
    private final String label;
    private final Integer confidencePercentage;
    private final String model;

    public ClassifyDocumentOutput(String documentId, String label, Integer confidencePercentage, String model) {
        this.documentId = Objects.requireNonNull(documentId, "documentId cannot be null");
        this.label = Objects.requireNonNull(label, "label cannot be null");
        this.confidencePercentage = Objects.requireNonNull(confidencePercentage, "confidencePercentage cannot be null");
        this.model = Objects.requireNonNull(model, "model cannot be null");
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getLabel() {
        return label;
    }

    public Integer getConfidencePercentage() {
        return confidencePercentage;
    }

    public String getModel() {
        return model;
    }

    @Override
    public String toString() {
        return "ClassifyDocumentOutput{" +
                "documentId='" + documentId + '\'' +
                ", label='" + label + '\'' +
                ", confidence=" + confidencePercentage + "%" +
                ", model='" + model + '\'' +
                '}';
    }
}
