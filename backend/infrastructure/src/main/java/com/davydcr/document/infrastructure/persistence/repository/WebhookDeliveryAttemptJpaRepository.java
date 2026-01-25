package com.davydcr.document.infrastructure.persistence.repository;

import com.davydcr.document.infrastructure.persistence.entity.WebhookDeliveryAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface WebhookDeliveryAttemptJpaRepository extends JpaRepository<WebhookDeliveryAttemptEntity, String> {

    List<WebhookDeliveryAttemptEntity> findByWebhookSubscriptionId(String webhookSubscriptionId);

    @Query(value = "SELECT a FROM WebhookDeliveryAttemptEntity a WHERE a.webhookSubscriptionId = :webhookId AND a.eventType = :eventType AND a.success = false ORDER BY a.attemptedAt DESC LIMIT 1")
    java.util.Optional<WebhookDeliveryAttemptEntity> findLastFailedAttempt(@Param("webhookId") String webhookId, @Param("eventType") String eventType);

    List<WebhookDeliveryAttemptEntity> findBySuccessFalseAndNextRetryAtIsNotNullAndNextRetryAtLessThanEqual(Instant now);

    int countByWebhookSubscriptionIdAndSuccess(String webhookSubscriptionId, Boolean success);

    @Query("SELECT COUNT(a) FROM WebhookDeliveryAttemptEntity a WHERE a.webhookSubscriptionId = :webhookId AND a.attemptedAt >= :since AND a.success = false")
    long countFailedAttemptsSince(@Param("webhookId") String webhookId, @Param("since") Instant since);
}
