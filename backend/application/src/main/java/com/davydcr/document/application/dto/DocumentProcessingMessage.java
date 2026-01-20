package com.davydcr.document.application.dto;

import java.io.Serializable;
import java.util.Objects;

/**
 * DTO para mensagem de processamento de documento na fila RabbitMQ
 * Serializado em JSON para transmissão pela fila
 */
public class DocumentProcessingMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String documentId;
    private String filePath;
    private String fileType;
    private String uploadedBy;
    private long timestamp;

    // Constructors
    public DocumentProcessingMessage() {
    }

    public DocumentProcessingMessage(String documentId, String filePath, String fileType, 
                                    String uploadedBy, long timestamp) {
        this.documentId = Objects.requireNonNull(documentId, "documentId cannot be null");
        this.filePath = Objects.requireNonNull(filePath, "filePath cannot be null");
        this.fileType = Objects.requireNonNull(fileType, "fileType cannot be null");
        this.uploadedBy = uploadedBy;
        this.timestamp = timestamp;
    }

    // Getters
    public String getDocumentId() {
        return documentId;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFileType() {
        return fileType;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // Setters (for JSON deserialization)
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "DocumentProcessingMessage{" +
                "documentId='" + documentId + '\'' +
                ", fileType='" + fileType + '\'' +
                ", uploadedBy='" + uploadedBy + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocumentProcessingMessage that = (DocumentProcessingMessage) o;
        return Objects.equals(documentId, that.documentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(documentId);
    }
}
