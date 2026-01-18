package com.davydcr.document.application.dto;

import java.util.Objects;

/**
 * DTO para entrada do caso de uso de processar documento.
 */
public class ProcessDocumentInput {

    private final String documentId;
    private final String filePath;
    private final String fileType;

    public ProcessDocumentInput(String documentId, String filePath, String fileType) {
        this.documentId = Objects.requireNonNull(documentId, "documentId cannot be null");
        this.filePath = Objects.requireNonNull(filePath, "filePath cannot be null");
        this.fileType = Objects.requireNonNull(fileType, "fileType cannot be null");
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFileType() {
        return fileType;
    }

    @Override
    public String toString() {
        return "ProcessDocumentInput{" +
                "documentId='" + documentId + '\'' +
                ", filePath='" + filePath + '\'' +
                ", fileType='" + fileType + '\'' +
                '}';
    }
}
