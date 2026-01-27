package com.davydcr.document.infrastructure.controller;

import com.davydcr.document.infrastructure.controller.dto.CreateWebhookRequest;
import com.davydcr.document.infrastructure.controller.dto.WebhookSubscriptionDTO;
import com.davydcr.document.infrastructure.persistence.entity.WebhookSubscriptionEntity;
import com.davydcr.document.infrastructure.persistence.repository.WebhookSubscriptionJpaRepository;
import com.davydcr.document.infrastructure.security.SecurityContextService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * REST API Controller para gerenciar Webhooks.
 * Endpoints para registrar, listar e deletar subscriptions.
 */
@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);

    private final WebhookSubscriptionJpaRepository webhookRepository;
    private final SecurityContextService securityContextService;

    public WebhookController(WebhookSubscriptionJpaRepository webhookRepository,
                           SecurityContextService securityContextService) {
        this.webhookRepository = Objects.requireNonNull(webhookRepository);
        this.securityContextService = Objects.requireNonNull(securityContextService);
    }

    /**
     * Registra um novo webhook.
     * POST /api/webhooks
     */
    @PostMapping
    public ResponseEntity<WebhookSubscriptionDTO> registerWebhook(
            @RequestBody CreateWebhookRequest request) {

        logger.info("Registering webhook: url={}, events={}", request.url(), request.eventTypes());

        // Validar URL
        if (!isValidUrl(request.url())) {
            logger.warn("Invalid webhook URL: {}", request.url());
            return ResponseEntity.badRequest().build();
        }

        // Extrair userId do Spring Security context
        String userId = securityContextService.getCurrentUserId();
        if (userId == null) {
            logger.warn("Unauthorized attempt to register webhook");
            return ResponseEntity.status(401).build();
        }

        // Criar entidade
        WebhookSubscriptionEntity webhook = new WebhookSubscriptionEntity(
                request.url(),
                userId,
                request.eventTypes()
        );

        // Salvar
        WebhookSubscriptionEntity saved = webhookRepository.save(webhook);

        // Retornar resposta
        WebhookSubscriptionDTO response = mapToDTO(saved);

        return ResponseEntity.created(URI.create("/api/webhooks/" + saved.getId()))
                .body(response);
    }

    /**
     * Lista webhooks do usuário autenticado.
     * GET /api/webhooks
     */
    @GetMapping
    public ResponseEntity<List<WebhookSubscriptionDTO>> listWebhooks() {

        // Extrair userId do contexto de segurança
        String userId = securityContextService.getCurrentUserId();
        if (userId == null) {
            logger.warn("Unauthorized attempt to list webhooks");
            return ResponseEntity.status(401).build();
        }

        logger.info("Listing webhooks for user: {}", userId);

        List<WebhookSubscriptionEntity> webhooks = webhookRepository.findByUserId(userId);

        List<WebhookSubscriptionDTO> responses = webhooks.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * Desregistra um webhook.
     * DELETE /api/webhooks/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> unregisterWebhook(
            @PathVariable String id) {

        logger.info("Unregistering webhook: id={}", id);

        // Extrair userId do contexto de segurança
        String userId = securityContextService.getCurrentUserId();
        if (userId == null) {
            logger.warn("Unauthorized attempt to unregister webhook: id={}", id);
            return ResponseEntity.status(401).build();
        }

        // Verificar se webhook existe e pertence ao usuário
        WebhookSubscriptionEntity webhook = webhookRepository.findById(id)
                .orElse(null);

        if (webhook == null || !webhook.getUserId().equals(userId)) {
            logger.warn("Webhook not found or not owned by user: id={}, userId={}", id, userId);
            return ResponseEntity.notFound().build();
        }

        // Deletar
        webhookRepository.deleteById(id);

        logger.info("Webhook unregistered successfully: id={}", id);

        return ResponseEntity.noContent().build();
    }

    /**
     * Converte entidade para DTO.
     */
    private WebhookSubscriptionDTO mapToDTO(WebhookSubscriptionEntity entity) {
        return new WebhookSubscriptionDTO(
                entity.getId(),
                entity.getUrl(),
                entity.getEventTypes(),
                entity.getActive(),
                entity.getCreatedAt(),
                entity.getLastTriggeredAt()
        );
    }

    /**
     * Valida URL de webhook.
     */
    private boolean isValidUrl(String url) {
        try {
            new URI(url).toURL();
            return url.startsWith("http://") || url.startsWith("https://");
        } catch (Exception e) {
            return false;
        }
    }
}
