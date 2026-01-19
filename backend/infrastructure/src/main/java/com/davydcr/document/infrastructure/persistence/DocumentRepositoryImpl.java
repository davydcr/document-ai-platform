package com.davydcr.document.infrastructure.persistence;

import com.davydcr.document.application.port.DocumentRepository;
import com.davydcr.document.domain.model.Document;
import com.davydcr.document.domain.model.DocumentId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class DocumentRepositoryImpl implements DocumentRepository {

    private final DocumentJpaRepository jpaRepository;

    public DocumentRepositoryImpl(DocumentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Document save(Document document) {
        // Para simplificar, assumimos que o usuário é "system"
        // Em produção, viria do contexto de segurança
        DocumentJpaEntity entity = DocumentJpaEntity.from(document, "system");
        DocumentJpaEntity saved = jpaRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<Document> findById(DocumentId documentId) {
        return jpaRepository.findById(documentId.value().toString())
                .map(DocumentJpaEntity::toDomain);
    }

    @Override
    public List<Document> findAll() {
        return jpaRepository.findAll().stream()
                .map(DocumentJpaEntity::toDomain)
                .toList();
    }

    @Override
    public void delete(DocumentId documentId) {
        jpaRepository.deleteById(documentId.value().toString());
    }
}
