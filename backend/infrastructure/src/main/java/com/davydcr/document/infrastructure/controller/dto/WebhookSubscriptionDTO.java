package com.davydcr.document.infrastructure.controller.dto;

import java.time.Instant;

public record WebhookSubscriptionDTO(
        String id,
        String url,
        String eventTypes,
        Boolean active,
        Instant createdAt,
        Instant lastTriggeredAt
) {
}
