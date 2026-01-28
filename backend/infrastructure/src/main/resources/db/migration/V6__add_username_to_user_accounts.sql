-- V6__add_username_to_user_accounts.sql: Adiciona coluna username com valor padrão
-- Criado em: 2026-01-27
-- Descrição: Garante compatibilidade com Hibernate que pode tentar inserir username

-- ============================================================
-- Adicionar coluna username com NULL se não existir
-- ============================================================
ALTER TABLE user_accounts ADD COLUMN IF NOT EXISTS username VARCHAR(255);

-- ============================================================
-- Atualizar registros com username = email
-- ============================================================
UPDATE user_accounts SET username = email WHERE username IS NULL;

-- ============================================================
-- SET NOT NULL para manter compatibilidade
-- ============================================================
ALTER TABLE user_accounts ALTER COLUMN username SET NOT NULL;

