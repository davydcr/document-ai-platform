-- V1__initial_schema.sql: Schema inicial do banco de dados
-- Criado em: 2026-01-19
-- Descri​ção: Tabelas principais para Document AI Platform

-- ============================================================
-- Tabela: documents
-- Descrição: Armazena informações dos documentos processados
-- ============================================================
CREATE TABLE documents (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    content_path VARCHAR(500),
    status VARCHAR(50) NOT NULL,
    file_size_bytes BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_user_id UUID,
    CONSTRAINT check_valid_type CHECK (type IN ('PDF', 'IMAGE', 'TXT')),
    CONSTRAINT check_valid_status CHECK (status IN ('RECEIVED', 'PROCESSING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX idx_documents_status ON documents(status);
CREATE INDEX idx_documents_created_at ON documents(created_at);
CREATE INDEX idx_documents_type ON documents(type);

-- ============================================================
-- Tabela: document_classifications
-- Descrição: Armazena resultados de classificação dos documentos
-- ============================================================
CREATE TABLE document_classifications (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    classification_label VARCHAR(255),
    confidence DECIMAL(5, 4),
    classification_status VARCHAR(50) NOT NULL,
    model_used VARCHAR(100),
    classified_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_valid_confidence CHECK (confidence >= 0 AND confidence <= 1),
    CONSTRAINT check_valid_class_status CHECK (classification_status IN ('PENDING', 'CLASSIFIED', 'FAILED')),
    CONSTRAINT fk_doc_classification FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
);

CREATE INDEX idx_document_classifications_document_id ON document_classifications(document_id);
CREATE INDEX idx_document_classifications_status ON document_classifications(classification_status);

-- ============================================================
-- Tabela: document_extraction_results
-- Descrição: Armazena resultados de extração de conteúdo
-- ============================================================
CREATE TABLE document_extraction_results (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    extracted_content TEXT,
    extraction_status VARCHAR(50) NOT NULL,
    ocr_engine VARCHAR(100),
    confidence DECIMAL(5, 4),
    extracted_at TIMESTAMP,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_valid_extract_status CHECK (extraction_status IN ('PENDING', 'EXTRACTED', 'FAILED')),
    CONSTRAINT check_valid_extract_confidence CHECK (confidence >= 0 AND confidence <= 1),
    CONSTRAINT fk_doc_extraction FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
);

CREATE INDEX idx_document_extraction_document_id ON document_extraction_results(document_id);
CREATE INDEX idx_document_extraction_status ON document_extraction_results(extraction_status);

-- ============================================================
-- Tabela: users
-- Descrição: Armazena informações de usuários
-- ============================================================
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_valid_role CHECK (role IN ('ADMIN', 'USER', 'VIEWER'))
);

CREATE INDEX idx_users_email ON users(email);

-- ============================================================
-- Tabela: document_processing_logs
-- Descrição: Log de eventos de processamento de documentos
-- ============================================================
CREATE TABLE document_processing_logs (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_status VARCHAR(50) NOT NULL,
    details TEXT,
    user_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_doc_log FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_log FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_document_processing_logs_document_id ON document_processing_logs(document_id);
CREATE INDEX idx_document_processing_logs_created_at ON document_processing_logs(created_at);
CREATE INDEX idx_document_processing_logs_event_type ON document_processing_logs(event_type);

-- ============================================================
-- Tabela: document_audit
-- Descrição: Auditoria de alterações em documentos
-- ============================================================
CREATE TABLE document_audit (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    change_type VARCHAR(50) NOT NULL,
    old_value TEXT,
    new_value TEXT,
    changed_by_user_id UUID,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_valid_change_type CHECK (change_type IN ('CREATE', 'UPDATE', 'DELETE', 'STATUS_CHANGE')),
    CONSTRAINT fk_doc_audit FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_audit FOREIGN KEY (changed_by_user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_document_audit_document_id ON document_audit(document_id);
CREATE INDEX idx_document_audit_changed_at ON document_audit(changed_at);

-- ============================================================
-- Comments / Documentação
-- ============================================================
COMMENT ON TABLE documents IS 'Armazena informações principais dos documentos processados pelo sistema';
COMMENT ON COLUMN documents.id IS 'ID único do documento (UUID)';
COMMENT ON COLUMN documents.name IS 'Nome original do arquivo do documento';
COMMENT ON COLUMN documents.type IS 'Tipo de documento (PDF, IMAGE, TXT)';
COMMENT ON COLUMN documents.status IS 'Status do processamento (RECEIVED, PROCESSING, COMPLETED, FAILED)';

COMMENT ON TABLE document_classifications IS 'Resultados de classificação automática dos documentos';
COMMENT ON TABLE document_extraction_results IS 'Resultados de extração de conteúdo (OCR/Text)';
COMMENT ON TABLE users IS 'Informações de usuários do sistema';
COMMENT ON TABLE document_processing_logs IS 'Log detalhado de eventos de processamento';
COMMENT ON TABLE document_audit IS 'Auditoria de todas as mudanças nos documentos';
