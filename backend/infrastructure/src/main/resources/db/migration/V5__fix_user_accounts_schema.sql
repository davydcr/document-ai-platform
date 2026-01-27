-- V5__fix_user_accounts_schema.sql: Corrige schema de user_accounts
-- Criado em: 2026-01-27
-- Descrição: Remove coluna username e adiciona first_name, last_name, updated_at

-- ============================================================
-- Remover constraint de username
-- ============================================================
ALTER TABLE user_accounts DROP CONSTRAINT IF EXISTS user_accounts_username_key;

-- ============================================================
-- Remover índice de username
-- ============================================================
DROP INDEX IF EXISTS idx_user_accounts_username;

-- ============================================================
-- Remover coluna username
-- ============================================================
ALTER TABLE user_accounts DROP COLUMN IF EXISTS username;

-- ============================================================
-- Adicionar colunas faltantes se não existirem
-- ============================================================
ALTER TABLE user_accounts ADD COLUMN IF NOT EXISTS first_name VARCHAR(100) NOT NULL DEFAULT 'User';
ALTER TABLE user_accounts ADD COLUMN IF NOT EXISTS last_name VARCHAR(100) NOT NULL DEFAULT 'System';
ALTER TABLE user_accounts ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- ============================================================
-- Remover coluna last_login_at se existir
-- ============================================================
ALTER TABLE user_accounts DROP COLUMN IF EXISTS last_login_at;

-- ============================================================
-- Garantir constraint NOT NULL no active
-- ============================================================
ALTER TABLE user_accounts ALTER COLUMN active SET NOT NULL;
ALTER TABLE user_accounts ALTER COLUMN active SET DEFAULT TRUE;
