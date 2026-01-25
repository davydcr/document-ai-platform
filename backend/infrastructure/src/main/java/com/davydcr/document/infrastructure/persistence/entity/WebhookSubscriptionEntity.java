package com.davydcr.document.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "webhook_subscriptions")
public class WebhookSubscriptionEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String eventTypes; // Comma-separated: "document.processed,document.failed"

    @Column(nullable = false)
    private Boolean active;

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant lastTriggeredAt;

    @Column
    private Integer failureCount;

    public WebhookSubscriptionEntity() {
    }

    public WebhookSubscriptionEntity(String url, String userId, String eventTypes) {
        this.id = UUID.randomUUID().toString();
        this.url = url;
        this.userId = userId;
        this.eventTypes = eventTypes;
        this.active = true;
        this.createdAt = Instant.now();
        this.failureCount = 0;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public String getUserId() {
        return userId;
    }

    public String getEventTypes() {
        return eventTypes;
    }

    public Boolean getActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastTriggeredAt() {
        return lastTriggeredAt;
    }

    public Integer getFailureCount() {
        return failureCount;
    }

    // Setters
    public void setUrl(String url) {
        this.url = url;
    }

    public void setEventTypes(String eventTypes) {
        this.eventTypes = eventTypes;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public void setLastTriggeredAt(Instant lastTriggeredAt) {
        this.lastTriggeredAt = lastTriggeredAt;
    }

    public void setFailureCount(Integer failureCount) {
        this.failureCount = failureCount;
    }

    public void incrementFailureCount() {
        this.failureCount = (failureCount == null ? 0 : failureCount) + 1;
    }

    public void resetFailureCount() {
        this.failureCount = 0;
    }
}
