package com.davydcr.document.domain.model;

import com.davydcr.document.domain.exception.DomainException;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

import java.util.Map;

class DocumentTest {
    @Test
    void shouldProcessDocumentSuccessfully() {
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

        assertEquals(DocumentStatus.PROCESSED, doc.getStatus());
        assertEquals(1, doc.getProcessingHistory().size());
    }
}
