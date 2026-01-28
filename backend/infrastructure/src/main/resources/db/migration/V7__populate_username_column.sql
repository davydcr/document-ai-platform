-- V7__populate_username_column.sql: Popula coluna username com valor padrão
-- Criado em: 2026-01-27
-- Descrição: Define username = email para registros sem username

-- ============================================================
-- Atualizar registros com username vazio/null durante seed
-- ============================================================
UPDATE user_accounts SET username = email WHERE username IS NULL OR username = '';
