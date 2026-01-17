package com.davydcr.document.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class ProcessingResult {

    private final UUID id;
    private final Instant processedAt;
    private final ProcessingStatus status;
    private final Map<String, Object> data;
    private final String modelVersion;

    public ProcessingResult(ProcessingStatus status,
                            Map<String, Object> data,
                            String modelVersion) {

        this.id = UUID.randomUUID();
        this.processedAt = Instant.now();
        this.status = Objects.requireNonNull(status);
        this.data = Objects.requireNonNull(data);
        this.modelVersion = Objects.requireNonNull(modelVersion);
    }

    public boolean isSuccess() {
        return status == ProcessingStatus.SUCCESS;
    }

    // getters
}
