package com.davydcr.document.domain.model;

import com.davydcr.document.domain.exception.DomainException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Document {

    private final DocumentId id;
    private final String originalName;
    private final DocumentType type;
    private DocumentStatus status;
    private final Instant createdAt;
    private final List<ProcessingResult> processingHistory = new ArrayList<>();

    public Document(DocumentId id,
                    String originalName,
                    DocumentType type) {

        this.id = Objects.requireNonNull(id);
        this.originalName = Objects.requireNonNull(originalName);
        this.type = Objects.requireNonNull(type);
        this.status = DocumentStatus.UPLOADED;
        this.createdAt = Instant.now();
    }

    public void queue() {
        ensureStatus(DocumentStatus.UPLOADED);
        this.status = DocumentStatus.QUEUED;
    }

    public void startProcessing() {
        ensureStatus(DocumentStatus.QUEUED);
        this.status = DocumentStatus.PROCESSING;
    }

    public void finishProcessing(ProcessingResult result) {
        ensureStatus(DocumentStatus.PROCESSING);
        this.processingHistory.add(result);
        this.status = result.isSuccess()
                ? DocumentStatus.PROCESSED
                : DocumentStatus.FAILED;
    }

    public void reprocess() {
        if (status != DocumentStatus.PROCESSED && status != DocumentStatus.FAILED) {
            throw new DomainException("Document cannot be reprocessed in state: " + status) {};
        }
        this.status = DocumentStatus.QUEUED;
    }

    private void ensureStatus(DocumentStatus expected) {
        if (this.status != expected) {
            throw new DomainException(
                    "Invalid status transition from " + status + " to " + expected
            ) {};
        }
    }

    public List<ProcessingResult> getProcessingHistory() {
        return Collections.unmodifiableList(processingHistory);
    }

    // getters
    public DocumentId getId() {
        return id;
    }

    public String getOriginalName() {
        return originalName;
    }

    public DocumentType getType() {
        return type;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
