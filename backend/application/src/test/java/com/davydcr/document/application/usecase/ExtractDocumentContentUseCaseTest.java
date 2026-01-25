package com.davydcr.document.application.usecase;

import com.davydcr.document.application.dto.ExtractDocumentContentCommand;
import com.davydcr.document.application.dto.ExtractDocumentContentResult;
import com.davydcr.document.application.port.DocumentRepository;
import com.davydcr.document.application.port.OcrService;
import com.davydcr.document.domain.exception.DomainException;
import com.davydcr.document.domain.model.Document;
import com.davydcr.document.domain.model.DocumentId;
import com.davydcr.document.domain.model.DocumentType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ExtractDocumentContentUseCaseTest {

    private ExtractDocumentContentUseCase useCase;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private OcrService ocrService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new ExtractDocumentContentUseCase(documentRepository, ocrService);
    }

    @Test
    void should_extractContentSuccessfully_when_validCommandProvided() {
        DocumentId documentId = DocumentId.newId();
        Document document = new Document(documentId, "test.pdf", DocumentType.PDF);

        ExtractDocumentContentCommand command = new ExtractDocumentContentCommand(
                documentId,
                "/path/to/test.pdf",
                "Tesseract"
        );

        when(documentRepository.findById(documentId))
                .thenReturn(Optional.of(document));

        ExtractDocumentContentResult result = useCase.execute(command);

        assertThat(result.getDocumentId()).isEqualTo(documentId);
        assertThat(result.getPageCount()).isEqualTo(1);
        assertThat(result.getTextLength()).isGreaterThan(0);
        assertThat(result.getOcrEngine()).isEqualTo("Tesseract");

        verify(documentRepository).save(any(Document.class));
    }

    @Test
    void should_throwException_when_documentNotFound() {
        DocumentId documentId = DocumentId.newId();

        ExtractDocumentContentCommand command = new ExtractDocumentContentCommand(
                documentId,
                "/path/to/test.pdf",
                "Tesseract"
        );

        when(documentRepository.findById(documentId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Document not found");
    }

    @Test
    void should_throwException_when_nullCommand() {
        assertThatThrownBy(() -> useCase.execute(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_saveDocumentWithExtractedContent_when_successfulExtraction() {
        DocumentId documentId = DocumentId.newId();
        Document document = new Document(documentId, "test.pdf", DocumentType.PDF);

        ExtractDocumentContentCommand command = new ExtractDocumentContentCommand(
                documentId,
                "/path/to/test.pdf",
                "Tesseract"
        );

        when(documentRepository.findById(documentId))
                .thenReturn(Optional.of(document));

        useCase.execute(command);

        verify(documentRepository, times(1)).save(any(Document.class));
    }

    @Test
    void should_supportDifferentOcrEngines() {
        DocumentId documentId = DocumentId.newId();
        Document document = new Document(documentId, "test.pdf", DocumentType.PDF);

        ExtractDocumentContentCommand command = new ExtractDocumentContentCommand(
                documentId,
                "/path/to/test.pdf",
                "PaddleOCR"
        );

        when(documentRepository.findById(documentId))
                .thenReturn(Optional.of(document));

        ExtractDocumentContentResult result = useCase.execute(command);

        assertThat(result.getOcrEngine()).isEqualTo("PaddleOCR");
    }

    @Test
    void should_createReceivedDocument_when_executingExtraction() {
        DocumentId documentId = DocumentId.newId();
        Document document = new Document(documentId, "test.pdf", DocumentType.PDF);
        // Documento é criado em estado RECEIVED

        ExtractDocumentContentCommand command = new ExtractDocumentContentCommand(
                documentId,
                "/path/to/test.pdf",
                "Tesseract"
        );

        when(documentRepository.findById(documentId))
                .thenReturn(Optional.of(document));

        ExtractDocumentContentResult result = useCase.execute(command);

        assertThat(result).isNotNull();
        verify(documentRepository).save(any(Document.class));
    }
}
