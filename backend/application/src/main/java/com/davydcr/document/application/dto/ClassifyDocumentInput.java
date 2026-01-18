package com.davydcr.document.application.dto;

import java.util.Objects;

/**
 * DTO para entrada do caso de uso de classificar documento.
 */
public class ClassifyDocumentInput {

    private final String documentId;
    private final String text;

    public ClassifyDocumentInput(String documentId, String text) {
        this.documentId = Objects.requireNonNull(documentId, "documentId cannot be null");
        this.text = Objects.requireNonNull(text, "text cannot be null");
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return "ClassifyDocumentInput{" +
                "documentId='" + documentId + '\'' +
                ", textLength=" + text.length() +
                '}';
    }
}
