package com.davydcr.document.infrastructure.integration;

import com.davydcr.document.infrastructure.persistence.entity.WebhookSubscriptionEntity;
import com.davydcr.document.infrastructure.persistence.repository.WebhookSubscriptionJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Testes de Integração - Webhook")
class WebhookIntegrationTest {

    @Autowired
    private WebhookSubscriptionJpaRepository webhookRepository;

    @Test
    @DisplayName("Deve criar assinatura webhook com sucesso")
    void testCreateWebhookSubscription() {
        String userId = UUID.randomUUID().toString();
        WebhookSubscriptionEntity webhook = new WebhookSubscriptionEntity(
            "https://example.com/webhook",
            userId,
            "document.processed"
        );
        
        WebhookSubscriptionEntity saved = webhookRepository.save(webhook);

        assertThat(saved).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getUrl()).isEqualTo("https://example.com/webhook");
        assertThat(saved.getEventTypes()).isEqualTo("document.processed");
        assertThat(saved.getActive()).isTrue();
    }

    @Test
    @DisplayName("Deve recuperar webhook por ID")
    void testGetWebhookById() {
        String userId = UUID.randomUUID().toString();
        WebhookSubscriptionEntity webhook = new WebhookSubscriptionEntity(
            "https://example.com/webhook",
            userId,
            "document.processed"
        );
        WebhookSubscriptionEntity saved = webhookRepository.save(webhook);

        var found = webhookRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getUrl()).isEqualTo("https://example.com/webhook");
    }

    @Test
    @DisplayName("Deve listar webhooks ativos do usuário")
    void testListActiveUserWebhooks() {
        String userId = UUID.randomUUID().toString();
        for (int i = 0; i < 3; i++) {
            WebhookSubscriptionEntity webhook = new WebhookSubscriptionEntity(
                "https://example.com/webhook" + i,
                userId,
                "document.processed"
            );
            webhookRepository.save(webhook);
        }

        var webhooks = webhookRepository.findByUserIdAndActiveTrue(userId);

        assertThat(webhooks).hasSize(3);
        assertThat(webhooks).allMatch(w -> w.getUserId().equals(userId));
        assertThat(webhooks).allMatch(w -> w.getActive());
    }

    @Test
    @DisplayName("Deve listar todos os webhooks do usuário")
    void testListAllUserWebhooks() {
        String userId = UUID.randomUUID().toString();
        WebhookSubscriptionEntity active = new WebhookSubscriptionEntity(
            "https://example.com/active",
            userId,
            "document.processed"
        );
        WebhookSubscriptionEntity inactive = new WebhookSubscriptionEntity(
            "https://example.com/inactive",
            userId,
            "document.failed"
        );
        inactive.setActive(false);
        webhookRepository.save(active);
        webhookRepository.save(inactive);

        var webhooks = webhookRepository.findByUserId(userId);

        assertThat(webhooks).hasSize(2);
        assertThat(webhooks).anyMatch(w -> w.getActive());
        assertThat(webhooks).anyMatch(w -> !w.getActive());
    }

    @Test
    @DisplayName("Deve desativar webhook")
    void testDeactivateWebhook() {
        String userId = UUID.randomUUID().toString();
        WebhookSubscriptionEntity webhook = new WebhookSubscriptionEntity(
            "https://example.com/webhook",
            userId,
            "document.processed"
        );
        WebhookSubscriptionEntity saved = webhookRepository.save(webhook);

        saved.setActive(false);
        WebhookSubscriptionEntity updated = webhookRepository.save(saved);

        assertThat(updated.getActive()).isFalse();
    }

    @Test
    @DisplayName("Deve atualizar timestamp de disparo")
    void testUpdateLastTriggeredAt() {
        String userId = UUID.randomUUID().toString();
        WebhookSubscriptionEntity webhook = new WebhookSubscriptionEntity(
            "https://example.com/webhook",
            userId,
            "document.processed"
        );
        WebhookSubscriptionEntity saved = webhookRepository.save(webhook);

        Instant now = Instant.now();
        saved.setLastTriggeredAt(now);
        WebhookSubscriptionEntity updated = webhookRepository.save(saved);

        assertThat(updated.getLastTriggeredAt()).isNotNull();
    }

    @Test
    @DisplayName("Deve incrementar contador de falhas")
    void testIncrementFailureCount() {
        String userId = UUID.randomUUID().toString();
        WebhookSubscriptionEntity webhook = new WebhookSubscriptionEntity(
            "https://example.com/webhook",
            userId,
            "document.processed"
        );
        WebhookSubscriptionEntity saved = webhookRepository.save(webhook);

        int failureCount = saved.getFailureCount();
        saved.setFailureCount(failureCount + 1);
        WebhookSubscriptionEntity updated = webhookRepository.save(saved);

        assertThat(updated.getFailureCount()).isEqualTo(failureCount + 1);
    }

    @Test
    @DisplayName("Deve encontrar webhooks ativos para evento específico")
    void testFindActiveWebhooksForEvent() {
        String userId = UUID.randomUUID().toString();
        WebhookSubscriptionEntity webhook1 = new WebhookSubscriptionEntity(
            "https://example.com/webhook1",
            userId,
            "document.processed,document.failed"
        );
        WebhookSubscriptionEntity webhook2 = new WebhookSubscriptionEntity(
            "https://example.com/webhook2",
            userId,
            "document.failed"
        );
        webhookRepository.save(webhook1);
        webhookRepository.save(webhook2);

        var webhooks = webhookRepository.findActiveWebhooksForEvent("document.processed");

        assertThat(webhooks).hasSizeGreaterThanOrEqualTo(1);
        assertThat(webhooks).anyMatch(w -> w.getEventTypes().contains("document.processed"));
    }

    @Test
    @DisplayName("Deve contar webhooks por usuário")
    void testCountWebhooksByUser() {
        String userId = UUID.randomUUID().toString();
        for (int i = 0; i < 5; i++) {
            WebhookSubscriptionEntity webhook = new WebhookSubscriptionEntity(
                "https://example.com/webhook" + i,
                userId,
                "document.processed"
            );
            webhookRepository.save(webhook);
        }

        int count = webhookRepository.countByUserId(userId);

        assertThat(count).isEqualTo(5);
    }

    @Test
    @DisplayName("Deve buscar webhooks ativos globalmente")
    void testFindAllActiveWebhooks() {
        String userId = UUID.randomUUID().toString();
        String userId2 = UUID.randomUUID().toString();
        
        WebhookSubscriptionEntity webhook1 = new WebhookSubscriptionEntity(
            "https://example.com/webhook1",
            userId,
            "document.processed"
        );
        WebhookSubscriptionEntity webhook2 = new WebhookSubscriptionEntity(
            "https://example.com/webhook2",
            userId2,
            "document.processed"
        );
        WebhookSubscriptionEntity webhook3 = new WebhookSubscriptionEntity(
            "https://example.com/webhook3",
            userId,
            "document.failed"
        );
        webhook3.setActive(false);
        
        webhookRepository.save(webhook1);
        webhookRepository.save(webhook2);
        webhookRepository.save(webhook3);

        var activeWebhooks = webhookRepository.findByActiveTrue();

        assertThat(activeWebhooks).hasSizeGreaterThanOrEqualTo(2);
        assertThat(activeWebhooks).allMatch(w -> w.getActive());
    }

    @Test
    @DisplayName("Deve atualizar eventos de webhook")
    void testUpdateWebhookEventTypes() {
        String userId = UUID.randomUUID().toString();
        WebhookSubscriptionEntity webhook = new WebhookSubscriptionEntity(
            "https://example.com/webhook",
            userId,
            "document.processed"
        );
        WebhookSubscriptionEntity saved = webhookRepository.save(webhook);

        saved.setEventTypes("document.processed,document.failed");
        WebhookSubscriptionEntity updated = webhookRepository.save(saved);

        assertThat(updated.getEventTypes()).isEqualTo("document.processed,document.failed");
        assertThat(updated.getEventTypes()).contains("document.processed");
        assertThat(updated.getEventTypes()).contains("document.failed");
    }
}
