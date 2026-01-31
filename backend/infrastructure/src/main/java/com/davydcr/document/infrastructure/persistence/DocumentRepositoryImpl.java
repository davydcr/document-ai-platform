package com.davydcr.document.infrastructure.persistence;

import com.davydcr.document.application.port.DocumentRepository;
import com.davydcr.document.domain.model.Document;
import com.davydcr.document.domain.model.DocumentId;
import com.davydcr.document.infrastructure.security.SecurityContextService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class DocumentRepositoryImpl implements DocumentRepository {

    private static final Logger logger = LoggerFactory.getLogger(DocumentRepositoryImpl.class);

    private final DocumentJpaRepository jpaRepository;
    private final SecurityContextService securityContextService;

    public DocumentRepositoryImpl(DocumentJpaRepository jpaRepository,
                                   SecurityContextService securityContextService) {
        this.jpaRepository = jpaRepository;
        this.securityContextService = securityContextService;
    }

    @Override
    public Document save(Document document) {
        // Obter userId do contexto de segurança
        String userId = securityContextService.getCurrentUserId();
        if (userId == null) {
            userId = "system"; // fallback para processamento em background
            logger.warn("No authenticated user found, using 'system' as userId for document: {}", 
                       document.getId().value());
        } else {
            logger.info("Saving document {} for user {}", document.getId().value(), userId);
        }
        
        DocumentJpaEntity entity = DocumentJpaEntity.from(document, userId);
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
