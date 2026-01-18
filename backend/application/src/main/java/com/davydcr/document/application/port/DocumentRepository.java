package com.davydcr.document.application.port;

import com.davydcr.document.domain.model.Document;
import com.davydcr.document.domain.model.DocumentId;

import java.util.Optional;
import java.util.List;

/**
 * Port para persistência de documentos.
 * Implementação será na camada de infraestrutura (JPA/Hibernate).
 */
public interface DocumentRepository {

    Document save(Document document);

    Optional<Document> findById(DocumentId documentId);

    List<Document> findAll();

    void delete(DocumentId documentId);
}
