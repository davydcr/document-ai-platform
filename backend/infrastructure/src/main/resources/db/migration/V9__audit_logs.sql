-- Migration V9: Tabela de Auditoria para Logs de Autenticação
-- Rastreia todas as atividades de autenticação e acesso

CREATE TABLE IF NOT EXISTS audit_logs (
    id VARCHAR(36) PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    user_id VARCHAR(36),
    email VARCHAR(255),
    ip_address VARCHAR(45),
    user_agent TEXT,
    endpoint VARCHAR(255),
    method VARCHAR(10),
    status_code INT,
    error_message TEXT,
    details TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Índices para queries de auditoria
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_email ON audit_logs(email);
CREATE INDEX idx_audit_logs_event_type ON audit_logs(event_type);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
CREATE INDEX idx_audit_logs_ip_address ON audit_logs(ip_address);

-- Índice composto para queries comuns (usuário + tipo de evento + data)
CREATE INDEX idx_audit_logs_user_event_date ON audit_logs(user_id, event_type, created_at DESC);

-- Índice para alertas de brute force (mesmo IP, múltiplas falhas)
CREATE INDEX idx_audit_logs_ip_event_type ON audit_logs(ip_address, event_type, created_at DESC);
