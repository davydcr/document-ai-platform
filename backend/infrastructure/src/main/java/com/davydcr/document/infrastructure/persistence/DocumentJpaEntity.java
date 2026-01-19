package com.davydcr.document.infrastructure.persistence;

import com.davydcr.document.domain.model.*;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documents")
public class DocumentJpaEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String originalName;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DocumentType type;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DocumentStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private String userId;

    @Column(columnDefinition = "TEXT")
    private String extractedText;

    @Column
    private String classificationLabel;

    @Column
    private Integer classificationConfidence;

    public DocumentJpaEntity() {
    }

    public DocumentJpaEntity(String id, String originalName, DocumentType type, 
                            DocumentStatus status, Instant createdAt, String userId) {
        this.id = id;
        this.originalName = originalName;
        this.type = type;
        this.status = status;
        this.createdAt = createdAt;
        this.userId = userId;
    }

    public static DocumentJpaEntity from(Document document, String userId) {
        DocumentJpaEntity entity = new DocumentJpaEntity(
                document.getId().value().toString(),
                document.getOriginalName(),
                document.getType(),
                document.getStatus(),
                document.getCreatedAt(),
                userId
        );

        // Se houver histórico de processamento com extractedContent e classification
        if (!document.getProcessingHistory().isEmpty()) {
            ProcessingResult result = document.getProcessingHistory().get(
                    document.getProcessingHistory().size() - 1
            );

            if (result.getExtractedContent().isPresent()) {
                entity.extractedText = result.getExtractedContent().get().getFullText();
            }

            if (result.getClassification().isPresent()) {
                entity.classificationLabel = result.getClassification().get().getLabel().getValue();
                entity.classificationConfidence = result.getClassification().get().getConfidence().getPercentage();
            }
        }

        return entity;
    }

    public Document toDomain() {
        Document document = new Document(
                new DocumentId(UUID.fromString(this.id)),
                this.originalName,
                this.type
        );
        return document;
    }

    // Getters e Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public DocumentType getType() {
        return type;
    }

    public void setType(DocumentType type) {
        this.type = type;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public void setStatus(DocumentStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getExtractedText() {
        return extractedText;
    }

    public void setExtractedText(String extractedText) {
        this.extractedText = extractedText;
    }

    public String getClassificationLabel() {
        return classificationLabel;
    }

    public void setClassificationLabel(String classificationLabel) {
        this.classificationLabel = classificationLabel;
    }

    public Integer getClassificationConfidence() {
        return classificationConfidence;
    }

    public void setClassificationConfidence(Integer classificationConfidence) {
        this.classificationConfidence = classificationConfidence;
    }
}
