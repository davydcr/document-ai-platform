package com.davydcr.document.infrastructure.controller.dto;

import java.time.Instant;

public record DocumentDTO(
        String id,
        String originalName,
        String documentType,
        String status,
        String extractedText,
        String classificationLabel,
        Integer classificationConfidence,
        Instant createdAt
) {
}
