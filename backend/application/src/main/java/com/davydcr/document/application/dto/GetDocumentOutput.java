package com.davydcr.document.application.dto;

import java.util.Objects;

/**
 * DTO para saída do caso de uso de recuperar documento.
 */
public class GetDocumentOutput {

    private final String documentId;
    private final String originalName;
    private final String type;
    private final String status;
    private final String createdAt;
    private final Integer totalProcessingAttempts;
    private final Integer successfulProcessing;

    public GetDocumentOutput(String documentId, String originalName, String type, String status,
                             String createdAt, Integer totalProcessingAttempts, Integer successfulProcessing) {
        this.documentId = Objects.requireNonNull(documentId, "documentId cannot be null");
        this.originalName = Objects.requireNonNull(originalName, "originalName cannot be null");
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.totalProcessingAttempts = Objects.requireNonNull(totalProcessingAttempts, "totalProcessingAttempts cannot be null");
        this.successfulProcessing = Objects.requireNonNull(successfulProcessing, "successfulProcessing cannot be null");
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getType() {
        return type;
    }

    public String getStatus() {
        return status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public Integer getTotalProcessingAttempts() {
        return totalProcessingAttempts;
    }

    public Integer getSuccessfulProcessing() {
        return successfulProcessing;
    }

    @Override
    public String toString() {
        return "GetDocumentOutput{" +
                "documentId='" + documentId + '\'' +
                ", originalName='" + originalName + '\'' +
                ", type='" + type + '\'' +
                ", status='" + status + '\'' +
                ", attempts=" + totalProcessingAttempts +
                ", successful=" + successfulProcessing +
                '}';
    }
}
