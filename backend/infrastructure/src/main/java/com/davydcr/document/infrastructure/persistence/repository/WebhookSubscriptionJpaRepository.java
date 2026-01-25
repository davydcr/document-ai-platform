package com.davydcr.document.infrastructure.persistence.repository;

import com.davydcr.document.infrastructure.persistence.entity.WebhookSubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebhookSubscriptionJpaRepository extends JpaRepository<WebhookSubscriptionEntity, String> {

    List<WebhookSubscriptionEntity> findByUserIdAndActiveTrue(String userId);

    List<WebhookSubscriptionEntity> findByUserId(String userId);

    @Query("SELECT w FROM WebhookSubscriptionEntity w WHERE w.userId = :userId AND w.active = true AND w.eventTypes LIKE %:eventType%")
    List<WebhookSubscriptionEntity> findActiveWebhooksForEvent(
            @Param("userId") String userId,
            @Param("eventType") String eventType
    );

    List<WebhookSubscriptionEntity> findByActiveTrue();

    int countByUserId(String userId);
}
