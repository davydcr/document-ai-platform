package com.davydcr.document.infrastructure.controller.dto;

public record CreateWebhookRequest(
        String url,
        String eventTypes
) {
}
