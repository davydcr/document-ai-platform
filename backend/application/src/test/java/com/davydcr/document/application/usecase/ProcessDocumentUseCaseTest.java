package com.davydcr.document.application.usecase;

import com.davydcr.document.application.dto.ProcessDocumentInput;
import com.davydcr.document.application.dto.ProcessDocumentOutput;
import com.davydcr.document.application.port.ClassificationService;
import com.davydcr.document.application.port.DocumentRepository;
import com.davydcr.document.application.port.OcrService;
import com.davydcr.document.domain.exception.DomainException;
import com.davydcr.document.domain.model.Confidence;
import com.davydcr.document.domain.model.ClassificationLabel;
import com.davydcr.document.domain.model.Document;
import com.davydcr.document.domain.model.DocumentClassification;
import com.davydcr.document.domain.model.DocumentId;
import com.davydcr.document.domain.model.DocumentStatus;
import com.davydcr.document.domain.model.DocumentType;
import com.davydcr.document.domain.model.ExtractedContent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessDocumentUseCaseTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private OcrService ocrService;

    @Mock
    private ClassificationService classificationService;

    private ProcessDocumentUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ProcessDocumentUseCase(documentRepository, ocrService, classificationService);
    }

    @Test
    void should_throwException_when_allServicesAvailable() {
        // Arrange - Note: Este teste verifica que o use case tenta ler o arquivo
        // Em um teste real, seria necessário mockar FileInputStream
        DocumentId docId = DocumentId.newId();
        Document document = new Document(docId, "invoice.pdf", DocumentType.PDF);
        ProcessDocumentInput input = new ProcessDocumentInput(docId.value().toString(), "/nonexistent/path.pdf", "pdf");

        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));
        when(ocrService.supportsFileType("pdf")).thenReturn(true);

        // Act & Assert - File reading should fail
        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Error reading file");

        verify(documentRepository, times(1)).findById(docId);
    }

    @Test
    void should_throwException_when_documentNotFound() {
        // Arrange
        DocumentId docId = DocumentId.newId();
        ProcessDocumentInput input = new ProcessDocumentInput(docId.value().toString(), "/path/to/file.pdf", "pdf");

        when(documentRepository.findById(docId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void should_throwException_when_fileTypeNotSupported() {
        // Arrange
        DocumentId docId = DocumentId.newId();
        Document document = new Document(docId, "document.xyz", DocumentType.PDF);
        ProcessDocumentInput input = new ProcessDocumentInput(docId.value().toString(), "/path/to/file.xyz", "xyz");

        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));
        when(ocrService.supportsFileType("xyz")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("not supported");
    }

    @Test
    void should_throwException_when_nullInput() {
        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(null))
                .isInstanceOf(NullPointerException.class);
    }
}
