package com.davydcr.document.infrastructure.integration;

import com.davydcr.document.infrastructure.persistence.DocumentJpaEntity;
import com.davydcr.document.infrastructure.persistence.DocumentJpaRepository;
import com.davydcr.document.domain.model.DocumentType;
import com.davydcr.document.domain.model.DocumentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Testes de Integração - Documento")
class DocumentIntegrationTest {

    @Autowired
    private DocumentJpaRepository documentRepository;

    @Test
    @DisplayName("Deve criar e persistir documento com sucesso")
    void testCreateDocument() {
        String userId = UUID.randomUUID().toString();
        DocumentJpaEntity doc = new DocumentJpaEntity(
            UUID.randomUUID().toString(),
            "invoice-2024.pdf",
            DocumentType.PDF,
            DocumentStatus.RECEIVED,
            Instant.now(),
            userId
        );
        
        DocumentJpaEntity saved = documentRepository.save(doc);
        
        assertThat(saved).isNotNull();
        assertThat(saved.getOriginalName()).isEqualTo("invoice-2024.pdf");
        assertThat(saved.getStatus()).isEqualTo(DocumentStatus.RECEIVED);
        assertThat(saved.getType()).isEqualTo(DocumentType.PDF);
    }

    @Test
    @DisplayName("Deve recuperar documento por ID")
    void testGetDocumentById() {
        String userId = UUID.randomUUID().toString();
        DocumentJpaEntity doc = new DocumentJpaEntity(
            UUID.randomUUID().toString(),
            "test.pdf",
            DocumentType.PDF,
            DocumentStatus.RECEIVED,
            Instant.now(),
            userId
        );
        DocumentJpaEntity saved = documentRepository.save(doc);

        var found = documentRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getOriginalName()).isEqualTo("test.pdf");
    }

    @Test
    @DisplayName("Deve atualizar status do documento")
    void testUpdateDocumentStatus() {
        String userId = UUID.randomUUID().toString();
        DocumentJpaEntity doc = new DocumentJpaEntity(
            UUID.randomUUID().toString(),
            "test.pdf",
            DocumentType.PDF,
            DocumentStatus.RECEIVED,
            Instant.now(),
            userId
        );
        DocumentJpaEntity saved = documentRepository.save(doc);

        saved.setStatus(DocumentStatus.PROCESSING);
        DocumentJpaEntity updated = documentRepository.save(saved);

        assertThat(updated.getStatus()).isEqualTo(DocumentStatus.PROCESSING);
    }

    @Test
    @DisplayName("Deve persistir texto extraído de OCR")
    void testPersistExtractedText() {
        String userId = UUID.randomUUID().toString();
        DocumentJpaEntity doc = new DocumentJpaEntity(
            UUID.randomUUID().toString(),
            "test.pdf",
            DocumentType.PDF,
            DocumentStatus.PROCESSING,
            Instant.now(),
            userId
        );
        String extractedText = "This is extracted text from OCR";
        doc.setExtractedText(extractedText);

        DocumentJpaEntity saved = documentRepository.save(doc);

        assertThat(saved.getExtractedText()).isEqualTo(extractedText);
    }

    @Test
    @DisplayName("Deve atualizar classificação do documento")
    void testUpdateClassification() {
        String userId = UUID.randomUUID().toString();
        DocumentJpaEntity doc = new DocumentJpaEntity(
            UUID.randomUUID().toString(),
            "test.pdf",
            DocumentType.PDF,
            DocumentStatus.PROCESSING,
            Instant.now(),
            userId
        );
        DocumentJpaEntity saved = documentRepository.save(doc);

        saved.setClassificationLabel("LEGAL_DOCUMENT");
        saved.setClassificationConfidence(95);
        DocumentJpaEntity updated = documentRepository.save(saved);

        assertThat(updated.getClassificationLabel()).isEqualTo("LEGAL_DOCUMENT");
        assertThat(updated.getClassificationConfidence()).isEqualTo(95);
    }

    @Test
    @DisplayName("Deve listar documentos do usuário com paginação")
    void testListDocumentsByUser() {
        String userId = UUID.randomUUID().toString();
        for (int i = 0; i < 5; i++) {
            DocumentJpaEntity doc = new DocumentJpaEntity(
                UUID.randomUUID().toString(),
                "doc" + i + ".pdf",
                DocumentType.PDF,
                DocumentStatus.RECEIVED,
                Instant.now(),
                userId
            );
            documentRepository.save(doc);
        }

        Page<DocumentJpaEntity> page = documentRepository.findByUserId(userId, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(5);
        assertThat(page.getTotalElements()).isEqualTo(5);
    }

    @Test
    @DisplayName("Deve filtrar documentos por status")
    void testFilterByStatus() {
        String userId = UUID.randomUUID().toString();
        for (int i = 0; i < 3; i++) {
            DocumentJpaEntity doc = new DocumentJpaEntity(
                UUID.randomUUID().toString(),
                "doc" + i + ".pdf",
                DocumentType.PDF,
                DocumentStatus.RECEIVED,
                Instant.now(),
                userId
            );
            documentRepository.save(doc);
        }

        Page<DocumentJpaEntity> page = documentRepository.findByUserIdAndStatus(
            userId, 
            DocumentStatus.RECEIVED.toString(),
            PageRequest.of(0, 10)
        );

        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getContent()).allMatch(doc -> doc.getStatus() == DocumentStatus.RECEIVED);
    }

    @Test
    @DisplayName("Deve filtrar documentos por tipo")
    void testFilterByType() {
        String userId = UUID.randomUUID().toString();
        DocumentJpaEntity pdf = new DocumentJpaEntity(
            UUID.randomUUID().toString(),
            "doc.pdf",
            DocumentType.PDF,
            DocumentStatus.RECEIVED,
            Instant.now(),
            userId
        );
        DocumentJpaEntity image = new DocumentJpaEntity(
            UUID.randomUUID().toString(),
            "doc.jpg",
            DocumentType.IMAGE,
            DocumentStatus.RECEIVED,
            Instant.now(),
            userId
        );
        documentRepository.save(pdf);
        documentRepository.save(image);

        Page<DocumentJpaEntity> page = documentRepository.findByUserIdAndType(
            userId,
            DocumentType.PDF.toString(),
            PageRequest.of(0, 10)
        );

        assertThat(page.getContent()).allMatch(doc -> doc.getType() == DocumentType.PDF);
    }

    @Test
    @DisplayName("Deve filtrar por status e tipo simultaneamente")
    void testFilterByStatusAndType() {
        String userId = UUID.randomUUID().toString();
        DocumentJpaEntity doc1 = new DocumentJpaEntity(
            UUID.randomUUID().toString(),
            "doc1.pdf",
            DocumentType.PDF,
            DocumentStatus.RECEIVED,
            Instant.now(),
            userId
        );
        DocumentJpaEntity doc2 = new DocumentJpaEntity(
            UUID.randomUUID().toString(),
            "doc2.pdf",
            DocumentType.PDF,
            DocumentStatus.PROCESSING,
            Instant.now(),
            userId
        );
        documentRepository.save(doc1);
        documentRepository.save(doc2);

        Page<DocumentJpaEntity> page = documentRepository.findByUserIdAndStatusAndType(
            userId,
            DocumentStatus.RECEIVED.toString(),
            DocumentType.PDF.toString(),
            PageRequest.of(0, 10)
        );

        assertThat(page.getContent()).allMatch(doc -> 
            doc.getStatus() == DocumentStatus.RECEIVED && doc.getType() == DocumentType.PDF
        );
    }

    @Test
    @DisplayName("Deve deletar documento")
    void testDeleteDocument() {
        String userId = UUID.randomUUID().toString();
        DocumentJpaEntity doc = new DocumentJpaEntity(
            UUID.randomUUID().toString(),
            "test.pdf",
            DocumentType.PDF,
            DocumentStatus.RECEIVED,
            Instant.now(),
            userId
        );
        DocumentJpaEntity saved = documentRepository.save(doc);

        documentRepository.deleteById(saved.getId());

        var found = documentRepository.findById(saved.getId());
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Deve recuperar múltiplos documentos do usuário")
    void testFindMultipleDocumentsByUser() {
        String userId = UUID.randomUUID().toString();
        for (int i = 0; i < 3; i++) {
            DocumentJpaEntity doc = new DocumentJpaEntity(
                UUID.randomUUID().toString(),
                "doc" + i + ".pdf",
                DocumentType.PDF,
                DocumentStatus.RECEIVED,
                Instant.now(),
                userId
            );
            documentRepository.save(doc);
        }

        var docs = documentRepository.findByUserId(userId);

        assertThat(docs).hasSize(3);
        assertThat(docs).allMatch(d -> d.getUserId().equals(userId));
    }

    @Test
    @DisplayName("Deve listar documentos com status específico")
    void testFindDocumentsByStatusAndUser() {
        String userId = UUID.randomUUID().toString();
        DocumentJpaEntity received = new DocumentJpaEntity(
            UUID.randomUUID().toString(),
            "received.pdf",
            DocumentType.PDF,
            DocumentStatus.RECEIVED,
            Instant.now(),
            userId
        );
        DocumentJpaEntity processing = new DocumentJpaEntity(
            UUID.randomUUID().toString(),
            "processing.pdf",
            DocumentType.PDF,
            DocumentStatus.PROCESSING,
            Instant.now(),
            userId
        );
        documentRepository.save(received);
        documentRepository.save(processing);

        var docs = documentRepository.findByStatusAndUserId(
            DocumentStatus.RECEIVED.toString(),
            userId
        );

        assertThat(docs).hasSize(1);
        assertThat(docs.get(0).getStatus()).isEqualTo(DocumentStatus.RECEIVED);
    }
}
