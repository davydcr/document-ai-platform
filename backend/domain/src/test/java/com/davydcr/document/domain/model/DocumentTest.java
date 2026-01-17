package com.davydcr.document.domain.model;

import com.davydcr.document.domain.exception.DomainException;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

import java.util.Map;

class DocumentTest {

    @Test
    void should_processDocumentSuccessfully_when_stateTransitionsValid() {
        Document doc = new Document(
            DocumentId.newId(),
            "contrato.pdf",
            DocumentType.PDF
        );

        doc.queue();
        doc.startProcessing();

        ProcessingResult result = new ProcessingResult(
            ProcessingStatus.SUCCESS,
            Map.of("summary", "Contrato válido"),
            "llama3"
        );

        doc.finishProcessing(result);

        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.PROCESSED);
        assertThat(doc.getProcessingHistory()).hasSize(1);
    }

    @Test
    void should_processDocumentWithExtraction_when_contentExtracted() {
        Document doc = new Document(
            DocumentId.newId(),
            "document.pdf",
            DocumentType.PDF
        );

        ExtractedContent extracted = new ExtractedContent(
            "Extracted text content",
            3,
            "Tesseract"
        );

        ProcessingResult result = new ProcessingResult(
            ProcessingStatus.SUCCESS,
            Map.of("text", "extracted"),
            "tesseract-v5",
            extracted,
            null
        );

        doc.queue();
        doc.startProcessing();
        doc.finishProcessing(result);

        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.PROCESSED);
        assertThat(result.getExtractedContent()).isPresent();
        assertThat(result.getExtractedContent().get().getFullText()).isEqualTo("Extracted text content");
    }

    @Test
    void should_processDocumentWithClassification_when_classified() {
        Document doc = new Document(
            DocumentId.newId(),
            "invoice.pdf",
            DocumentType.PDF
        );

        ClassificationLabel label = ClassificationLabel.of("Invoice");
        Confidence confidence = Confidence.of(95);
        DocumentClassification classification = new DocumentClassification(
            label,
            confidence,
            "llama3"
        );

        ProcessingResult result = new ProcessingResult(
            ProcessingStatus.SUCCESS,
            Map.of("class", "invoice"),
            "llama3",
            null,
            classification
        );

        doc.queue();
        doc.startProcessing();
        doc.finishProcessing(result);

        assertThat(result.getClassification()).isPresent();
        assertThat(result.getClassification().get().getLabel().getValue()).isEqualTo("Invoice");
        assertThat(result.getClassification().get().getConfidence().getPercentage()).isEqualTo(95);
    }

    @Test
    void should_failedProcessing_when_resultIsFailed() {
        Document doc = new Document(
            DocumentId.newId(),
            "corrupted.pdf",
            DocumentType.PDF
        );

        ProcessingResult result = new ProcessingResult(
            ProcessingStatus.ERROR,
            Map.of("error", "Cannot extract text"),
            "tesseract-v5"
        );

        doc.queue();
        doc.startProcessing();
        doc.finishProcessing(result);

        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.FAILED);
    }

    @Test
    void should_reprocessDocument_when_previouslyProcessed() {
        Document doc = new Document(
            DocumentId.newId(),
            "document.pdf",
            DocumentType.PDF
        );

        doc.queue();
        doc.startProcessing();
        doc.finishProcessing(new ProcessingResult(
            ProcessingStatus.SUCCESS,
            Map.of(),
            "model-v1"
        ));

        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.PROCESSED);

        doc.reprocess();

        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.QUEUED);
    }

    @Test
    void should_throwException_when_invalidStateTransition() {
        Document doc = new Document(
            DocumentId.newId(),
            "document.pdf",
            DocumentType.PDF
        );

        assertThatThrownBy(doc::startProcessing)
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Invalid status transition");
    }
}
