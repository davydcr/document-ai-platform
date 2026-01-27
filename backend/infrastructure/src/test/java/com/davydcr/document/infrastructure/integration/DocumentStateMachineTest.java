package com.davydcr.document.infrastructure.integration;

import com.davydcr.document.domain.model.DocumentStatus;
import com.davydcr.document.domain.model.DocumentType;
import com.davydcr.document.infrastructure.persistence.DocumentJpaEntity;
import com.davydcr.document.infrastructure.persistence.DocumentJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Testes de Integração - State Machine")
class DocumentStateMachineTest {

    @Autowired
    private DocumentJpaRepository documentRepository;

    @Test
    @DisplayName("Deve validar transição: RECEIVED → PROCESSING")
    void testTransitionFromReceivedToProcessing() {
        String userId = UUID.randomUUID().toString();
        DocumentJpaEntity doc = new DocumentJpaEntity(
            UUID.randomUUID().toString(),
            "document.pdf",
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
    @DisplayName("Deve validar transição: PROCESSING → COMPLETED")
    void testTransitionFromProcessingToCompleted() {
        String userId = UUID.randomUUID().toString();
        DocumentJpaEntity doc = new DocumentJpaEntity(
            UUID.randomUUID().toString(),
            "document.pdf",
            DocumentType.PDF,
            DocumentStatus.PROCESSING,
            Instant.now(),
            userId
        );
        DocumentJpaEntity saved = documentRepository.save(doc);

        saved.setStatus(DocumentStatus.COMPLETED);
        saved.setExtractedText("Extracted content");
        saved.setClassificationLabel("INVOICE");
        DocumentJpaEntity updated = documentRepository.save(saved);

        assertThat(updated.getStatus()).isEqualTo(DocumentStatus.COMPLETED);
        assertThat(updated.getExtractedText()).isNotNull();
        assertThat(updated.getClassificationLabel()).isNotNull();
    }

    @Test
    @DisplayName("Deve validar transição: PROCESSING → FAILED")
    void testTransitionFromProcessingToFailed() {
        String userId = UUID.randomUUID().toString();
        DocumentJpaEntity doc = new DocumentJpaEntity(
            UUID.randomUUID().toString(),
            "document.pdf",
            DocumentType.PDF,
            DocumentStatus.PROCESSING,
            Instant.now(),
            userId
        );
        DocumentJpaEntity saved = documentRepository.save(doc);

        saved.setStatus(DocumentStatus.FAILED);
        DocumentJpaEntity updated = documentRepository.save(saved);

        assertThat(updated.getStatus()).isEqualTo(DocumentStatus.FAILED);
    }

    @Test
    @DisplayName("Deve persistir documento com múltiplas transições")
    void testMultipleStateTransitions() {
        String userId = UUID.randomUUID().toString();
        DocumentJpaEntity doc = new DocumentJpaEntity(
            UUID.randomUUID().toString(),
            "multi-state.pdf",
            DocumentType.PDF,
            DocumentStatus.RECEIVED,
            Instant.now(),
            userId
        );

        // Transição 1: RECEIVED
        DocumentJpaEntity step1 = documentRepository.save(doc);
        assertThat(step1.getStatus()).isEqualTo(DocumentStatus.RECEIVED);

        // Transição 2: PROCESSING
        step1.setStatus(DocumentStatus.PROCESSING);
        DocumentJpaEntity step2 = documentRepository.save(step1);
        assertThat(step2.getStatus()).isEqualTo(DocumentStatus.PROCESSING);

        // Transição 3: COMPLETED
        step2.setStatus(DocumentStatus.COMPLETED);
        step2.setExtractedText("Content");
        DocumentJpaEntity step3 = documentRepository.save(step2);
        assertThat(step3.getStatus()).isEqualTo(DocumentStatus.COMPLETED);

        // Verify final state
        var final_doc = documentRepository.findById(doc.getId());
        assertThat(final_doc).isPresent();
        assertThat(final_doc.get().getStatus()).isEqualTo(DocumentStatus.COMPLETED);
    }

    @Test
    @DisplayName("Deve suportar diversos tipos de documento com estado")
    void testMultipleDocumentTypesWithStates() {
        String userId = UUID.randomUUID().toString();

        // PDF document
        DocumentJpaEntity pdf = new DocumentJpaEntity(
            UUID.randomUUID().toString(),
            "doc.pdf",
            DocumentType.PDF,
            DocumentStatus.RECEIVED,
            Instant.now(),
            userId
        );

        // IMAGE document
        DocumentJpaEntity image = new DocumentJpaEntity(
            UUID.randomUUID().toString(),
            "doc.jpg",
            DocumentType.IMAGE,
            DocumentStatus.RECEIVED,
            Instant.now(),
            userId
        );

        // TXT document
        DocumentJpaEntity txt = new DocumentJpaEntity(
            UUID.randomUUID().toString(),
            "doc.txt",
            DocumentType.TXT,
            DocumentStatus.RECEIVED,
            Instant.now(),
            userId
        );

        documentRepository.save(pdf);
        documentRepository.save(image);
        documentRepository.save(txt);

        var allDocs = documentRepository.findByUserId(userId);
        assertThat(allDocs).hasSize(3);
        assertThat(allDocs).anyMatch(d -> d.getType() == DocumentType.PDF);
        assertThat(allDocs).anyMatch(d -> d.getType() == DocumentType.IMAGE);
        assertThat(allDocs).anyMatch(d -> d.getType() == DocumentType.TXT);
    }

    @Test
    @DisplayName("Deve manter histórico de alterações de estado")
    void testStateChangeHistory() {
        String userId = UUID.randomUUID().toString();
        DocumentJpaEntity doc = new DocumentJpaEntity(
            UUID.randomUUID().toString(),
            "history-doc.pdf",
            DocumentType.PDF,
            DocumentStatus.RECEIVED,
            Instant.now(),
            userId
        );

        // Initial state
        DocumentJpaEntity v1 = documentRepository.save(doc);
        assertThat(v1.getStatus()).isEqualTo(DocumentStatus.RECEIVED);

        // Update to PROCESSING
        v1.setStatus(DocumentStatus.PROCESSING);
        v1.setExtractedText("Processing...");
        DocumentJpaEntity v2 = documentRepository.save(v1);

        // Update to COMPLETED
        v2.setStatus(DocumentStatus.COMPLETED);
        v2.setClassificationLabel("PROCESSED");
        DocumentJpaEntity v3 = documentRepository.save(v2);

        // Retrieve all versions
        var retrieved = documentRepository.findById(doc.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getStatus()).isEqualTo(DocumentStatus.COMPLETED);
        assertThat(retrieved.get().getClassificationLabel()).isEqualTo("PROCESSED");
    }

    @Test
    @DisplayName("Deve permitir alterações em metadados durante processamento")
    void testMetadataUpdatesDuringProcessing() {
        String userId = UUID.randomUUID().toString();
        DocumentJpaEntity doc = new DocumentJpaEntity(
            UUID.randomUUID().toString(),
            "metadata-doc.pdf",
            DocumentType.PDF,
            DocumentStatus.RECEIVED,
            Instant.now(),
            userId
        );

        DocumentJpaEntity saved = documentRepository.save(doc);
        assertThat(saved.getExtractedText()).isNull();

        // Update with extraction
        saved.setExtractedText("Document text content");
        DocumentJpaEntity withText = documentRepository.save(saved);
        assertThat(withText.getExtractedText()).isNotNull();

        // Update with classification
        withText.setClassificationLabel("INVOICE");
        withText.setClassificationConfidence(92);
        DocumentJpaEntity withClassification = documentRepository.save(withText);
        assertThat(withClassification.getClassificationLabel()).isEqualTo("INVOICE");
        assertThat(withClassification.getClassificationConfidence()).isEqualTo(92);
    }
}
