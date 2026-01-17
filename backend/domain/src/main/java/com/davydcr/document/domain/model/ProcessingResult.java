package com.davydcr.document.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class ProcessingResult {

    private final UUID id;
    private final Instant processedAt;
    private final ProcessingStatus status;
    private final Map<String, Object> data;
    private final String modelVersion;
    private final ExtractedContent extractedContent;
    private final DocumentClassification classification;

    public ProcessingResult(ProcessingStatus status,
                            Map<String, Object> data,
                            String modelVersion) {
        this(status, data, modelVersion, null, null);
    }

    public ProcessingResult(ProcessingStatus status,
                            Map<String, Object> data,
                            String modelVersion,
                            ExtractedContent extractedContent,
                            DocumentClassification classification) {

        this.id = UUID.randomUUID();
        this.processedAt = Instant.now();
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.data = Objects.requireNonNull(data, "data cannot be null");
        this.modelVersion = Objects.requireNonNull(modelVersion, "modelVersion cannot be null");
        this.extractedContent = extractedContent;
        this.classification = classification;
    }

    public boolean isSuccess() {
        return status == ProcessingStatus.SUCCESS;
    }

    public UUID getId() {
        return id;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public ProcessingStatus getStatus() {
        return status;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public Optional<ExtractedContent> getExtractedContent() {
        return Optional.ofNullable(extractedContent);
    }

    public Optional<DocumentClassification> getClassification() {
        return Optional.ofNullable(classification);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessingResult that = (ProcessingResult) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ProcessingResult{" +
                "id=" + id +
                ", status=" + status +
                ", modelVersion='" + modelVersion + '\'' +
                ", processedAt=" + processedAt +
                '}';
    }
}
