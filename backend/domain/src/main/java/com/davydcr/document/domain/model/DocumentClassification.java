package com.davydcr.document.domain.model;

import java.time.Instant;
import java.util.Objects;

public class DocumentClassification {

    private final ClassificationLabel label;
    private final Confidence confidence;
    private final String model;
    private final Instant classifiedAt;

    public DocumentClassification(ClassificationLabel label, Confidence confidence, String model) {
        this.label = Objects.requireNonNull(label, "label cannot be null");
        this.confidence = Objects.requireNonNull(confidence, "confidence cannot be null");
        this.model = Objects.requireNonNull(model, "model cannot be null");
        
        if (model.isBlank()) {
            throw new IllegalArgumentException("model cannot be blank");
        }
        
        this.classifiedAt = Instant.now();
    }

    public ClassificationLabel getLabel() {
        return label;
    }

    public Confidence getConfidence() {
        return confidence;
    }

    public String getModel() {
        return model;
    }

    public Instant getClassifiedAt() {
        return classifiedAt;
    }

    public boolean isReliable(int confidenceThreshold) {
        return confidence.isHighConfidence(confidenceThreshold);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocumentClassification that = (DocumentClassification) o;
        return Objects.equals(label, that.label) &&
                Objects.equals(confidence, that.confidence) &&
                Objects.equals(model, that.model) &&
                Objects.equals(classifiedAt, that.classifiedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, confidence, model, classifiedAt);
    }

    @Override
    public String toString() {
        return "DocumentClassification{" +
                "label=" + label +
                ", confidence=" + confidence +
                ", model='" + model + '\'' +
                '}';
    }
}
