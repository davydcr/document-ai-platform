package com.davydcr.document.domain.model;

import java.util.Objects;
import java.util.UUID;

public record DocumentId(UUID value) {

    public DocumentId {
        Objects.requireNonNull(value, "DocumentId cannot be null");
    }

    public static DocumentId newId() {
        return new DocumentId(UUID.randomUUID());
    }
}
