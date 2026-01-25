package com.davydcr.document.domain.model;

import com.davydcr.document.domain.event.DocumentProcessedEvent;
import com.davydcr.document.domain.event.DocumentStateChangedEvent;
import com.davydcr.document.domain.event.ProcessDocumentEvent;
import com.davydcr.document.domain.exception.DomainException;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

import java.util.Map;

class DocumentTest {

    @Test
    void should_startProcessing_when_documentReceived() {
        Document doc = new Document(
                DocumentId.newId(),
                "contrato.pdf",
                DocumentType.PDF
        );

        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.RECEIVED);

        doc.requestProcessing();

        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.PROCESSING);
        assertThat(doc.getDomainEvents()).hasSize(2)
                .anySatisfy(event -> assertThat(event).isInstanceOf(DocumentStateChangedEvent.class))
                .anySatisfy(event -> assertThat(event).isInstanceOf(ProcessDocumentEvent.class));
    }

    @Test
    void should_completeProcessing_when_processingFinished() {
        Document doc = new Document(
                DocumentId.newId(),
                "document.pdf",
                DocumentType.PDF
        );

        doc.requestProcessing();
        var events = new java.util.ArrayList<>(doc.getDomainEvents());
        doc.clearDomainEvents();

        ProcessingResult result = new ProcessingResult(
                ProcessingStatus.SUCCESS,
                Map.of("summary", "Contrato válido"),
                "llama3"
        );

        doc.completeProcessing(result);

        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.COMPLETED);
        assertThat(doc.getProcessingHistory()).hasSize(1);
        assertThat(doc.getDomainEvents()).hasSize(2)
                .anySatisfy(event -> assertThat(event).isInstanceOf(DocumentStateChangedEvent.class))
                .anySatisfy(event -> assertThat(event).isInstanceOf(DocumentProcessedEvent.class));
    }

    @Test
    void should_failProcessing_when_processingError() {
        Document doc = new Document(
                DocumentId.newId(),
                "corrupted.pdf",
                DocumentType.PDF
        );

        doc.requestProcessing();
        var events = new java.util.ArrayList<>(doc.getDomainEvents());
        doc.clearDomainEvents();

        doc.failProcessing("Cannot extract text from corrupted PDF");

        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.FAILED);
        assertThat(doc.getDomainEvents()).hasSize(2);
    }

    @Test
    void should_publishEvents_when_transitioningStates() {
        Document doc = new Document(
                DocumentId.newId(),
                "document.pdf",
                DocumentType.PDF
        );

        doc.requestProcessing();

        assertThat(doc.getDomainEvents()).hasSize(2);

        var stateChangedEvent = (DocumentStateChangedEvent) doc.getDomainEvents().get(0);
        assertThat(stateChangedEvent.previousStatus()).isEqualTo(DocumentStatus.RECEIVED);
        assertThat(stateChangedEvent.newStatus()).isEqualTo(DocumentStatus.PROCESSING);

        var processEvent = (ProcessDocumentEvent) doc.getDomainEvents().get(1);
        assertThat(processEvent.documentId()).isEqualTo(doc.getId().value().toString());
    }

    @Test
    void should_clearEvents_when_eventsCleared() {
        Document doc = new Document(
                DocumentId.newId(),
                "document.pdf",
                DocumentType.PDF
        );

        doc.requestProcessing();
        assertThat(doc.getDomainEvents()).hasSize(2);

        var events = new java.util.ArrayList<>(doc.getDomainEvents());
        doc.clearDomainEvents();
        assertThat(doc.getDomainEvents()).isEmpty();
    }

    @Test
    void should_throwException_when_invalidStateTransition() {
        Document doc = new Document(
                DocumentId.newId(),
                "document.pdf",
                DocumentType.PDF
        );

        // Try to complete processing without requesting it first
        ProcessingResult result = new ProcessingResult(
                ProcessingStatus.SUCCESS,
                Map.of(),
                "model-v1"
        );

        assertThatThrownBy(() -> doc.completeProcessing(result))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Cannot complete processing");
    }

    @Test
    void should_validateStateTransitions_with_canTransitionTo() {
        assertThat(DocumentStatus.RECEIVED.canTransitionTo(DocumentStatus.PROCESSING)).isTrue();
        assertThat(DocumentStatus.RECEIVED.canTransitionTo(DocumentStatus.COMPLETED)).isFalse();
        assertThat(DocumentStatus.PROCESSING.canTransitionTo(DocumentStatus.COMPLETED)).isTrue();
        assertThat(DocumentStatus.PROCESSING.canTransitionTo(DocumentStatus.FAILED)).isTrue();
        assertThat(DocumentStatus.COMPLETED.canTransitionTo(DocumentStatus.FAILED)).isFalse();
    }
}
