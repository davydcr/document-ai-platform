package com.davydcr.document.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "webhook_delivery_attempts")
public class WebhookDeliveryAttemptEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String webhookSubscriptionId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String eventPayload;

    @Column
    private Integer httpStatusCode;

    @Column(columnDefinition = "LONGTEXT")
    private String responseBody;

    @Column(nullable = false)
    private Instant attemptedAt;

    @Column
    private Instant nextRetryAt;

    @Column(nullable = false)
    private Integer attemptNumber;

    @Column
    private Boolean success;

    @Column
    private String errorMessage;

    public WebhookDeliveryAttemptEntity() {
    }

    public WebhookDeliveryAttemptEntity(String webhookSubscriptionId, String eventType, String eventPayload) {
        this.id = UUID.randomUUID().toString();
        this.webhookSubscriptionId = webhookSubscriptionId;
        this.eventType = eventType;
        this.eventPayload = eventPayload;
        this.attemptedAt = Instant.now();
        this.attemptNumber = 1;
        this.success = false;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getWebhookSubscriptionId() {
        return webhookSubscriptionId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getEventPayload() {
        return eventPayload;
    }

    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public Instant getAttemptedAt() {
        return attemptedAt;
    }

    public Instant getNextRetryAt() {
        return nextRetryAt;
    }

    public Integer getAttemptNumber() {
        return attemptNumber;
    }

    public Boolean getSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    // Setters
    public void setHttpStatusCode(Integer httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public void setNextRetryAt(Instant nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public void setAttemptNumber(Integer attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setAttemptedAt(Instant attemptedAt) {
        this.attemptedAt = attemptedAt;
    }

    public boolean isSuccess() {
        return success != null && success;
    }

    public void incrementAttemptNumber() {
        this.attemptNumber = (attemptNumber == null ? 1 : attemptNumber) + 1;
    }
}
