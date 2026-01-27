-- V2__authentication_schema.sql: Schema para autenticação
-- Criado em: 2026-01-25
-- Descrição: Tabelas para suporte a autenticação JWT

-- ============================================================
-- Tabela: user_accounts
-- Descrição: Armazena dados de autenticação dos usuários
-- ============================================================
CREATE TABLE user_accounts (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP,
    CONSTRAINT check_valid_email CHECK (email LIKE '%@%')
);

CREATE INDEX idx_user_accounts_username ON user_accounts(username);
CREATE INDEX idx_user_accounts_email ON user_accounts(email);
CREATE INDEX idx_user_accounts_active ON user_accounts(active);
