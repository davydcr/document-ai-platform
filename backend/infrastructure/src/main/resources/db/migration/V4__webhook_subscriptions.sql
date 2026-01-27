-- V3__webhook_subscriptions.sql: Webhooks para notificações de eventos
-- Criado em: 2026-01-25
-- Descrição: Suporte a webhooks para notificações em tempo real

-- ============================================================
-- Tabela: webhook_subscriptions
-- Descrição: Armazena subscriptions de webhooks dos usuários
-- ============================================================
CREATE TABLE webhook_subscriptions (
    id VARCHAR(36) PRIMARY KEY,
    url VARCHAR(2048) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    event_types VARCHAR(500) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_triggered_at TIMESTAMP,
    failure_count INTEGER DEFAULT 0,
    CONSTRAINT fk_webhook_user FOREIGN KEY (user_id) REFERENCES user_accounts(id) ON DELETE CASCADE,
    CONSTRAINT check_valid_url CHECK (url LIKE 'http%://%')
);

CREATE INDEX idx_webhook_subscriptions_user_id ON webhook_subscriptions(user_id);
CREATE INDEX idx_webhook_subscriptions_active ON webhook_subscriptions(active);
CREATE INDEX idx_webhook_subscriptions_created_at ON webhook_subscriptions(created_at);

-- ============================================================
-- Tabela: webhook_delivery_attempts
-- Descrição: Rastreia tentativas de entrega de webhooks
-- ============================================================
CREATE TABLE webhook_delivery_attempts (
    id VARCHAR(36) PRIMARY KEY,
    webhook_subscription_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_payload TEXT NOT NULL,
    http_status_code INTEGER,
    response_body TEXT,
    attempted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    next_retry_at TIMESTAMP,
    attempt_number INTEGER NOT NULL DEFAULT 1,
    success BOOLEAN DEFAULT FALSE,
    error_message VARCHAR(1000),
    CONSTRAINT fk_webhook_attempt FOREIGN KEY (webhook_subscription_id) REFERENCES webhook_subscriptions(id) ON DELETE CASCADE
);

CREATE INDEX idx_webhook_delivery_attempts_webhook_id ON webhook_delivery_attempts(webhook_subscription_id);
CREATE INDEX idx_webhook_delivery_attempts_event_type ON webhook_delivery_attempts(event_type);
CREATE INDEX idx_webhook_delivery_attempts_attempted_at ON webhook_delivery_attempts(attempted_at);
CREATE INDEX idx_webhook_delivery_attempts_next_retry_at ON webhook_delivery_attempts(next_retry_at);
CREATE INDEX idx_webhook_delivery_attempts_success ON webhook_delivery_attempts(success);
