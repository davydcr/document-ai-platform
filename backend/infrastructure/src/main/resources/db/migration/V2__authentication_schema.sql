-- V2__authentication_schema.sql: Schema para autenticação
-- Criado em: 2026-01-25
-- Descrição: Tabelas para suporte a autenticação e autorização

-- ============================================================
-- Tabela: roles
-- Descrição: Define roles/papéis no sistema
-- ============================================================
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- Tabela: user_accounts
-- Descrição: Armazena dados de autenticação dos usuários
-- ============================================================
CREATE TABLE user_accounts (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_valid_email CHECK (email LIKE '%@%')
);

CREATE INDEX idx_user_accounts_email ON user_accounts(email);
CREATE INDEX idx_user_accounts_active ON user_accounts(active);

-- ============================================================
-- Tabela: user_roles
-- Descrição: Associação entre usuários e roles (many-to-many)
-- ============================================================
CREATE TABLE user_roles (
    user_id VARCHAR(36) NOT NULL REFERENCES user_accounts(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);
