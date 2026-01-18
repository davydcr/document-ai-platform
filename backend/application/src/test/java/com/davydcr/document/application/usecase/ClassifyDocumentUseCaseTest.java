package com.davydcr.document.application.usecase;

import com.davydcr.document.application.dto.ClassifyDocumentInput;
import com.davydcr.document.application.dto.ClassifyDocumentOutput;
import com.davydcr.document.application.port.ClassificationService;
import com.davydcr.document.application.port.DocumentRepository;
import com.davydcr.document.domain.exception.DomainException;
import com.davydcr.document.domain.model.Confidence;
import com.davydcr.document.domain.model.ClassificationLabel;
import com.davydcr.document.domain.model.Document;
import com.davydcr.document.domain.model.DocumentClassification;
import com.davydcr.document.domain.model.DocumentId;
import com.davydcr.document.domain.model.DocumentType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClassifyDocumentUseCaseTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private ClassificationService classificationService;

    private ClassifyDocumentUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ClassifyDocumentUseCase(documentRepository, classificationService);
    }

    @Test
    void should_classifyDocumentSuccessfully_when_validTextProvided() {
        // Arrange
        DocumentId docId = DocumentId.newId();
        Document document = new Document(docId, "invoice.pdf", DocumentType.PDF);
        String text = "Invoice number 12345 dated 2025-01-17";

        ClassifyDocumentInput input = new ClassifyDocumentInput(docId.value().toString(), text);

        DocumentClassification classification = new DocumentClassification(
                ClassificationLabel.of("Invoice"),
                Confidence.of(88),
                "llama3"
        );

        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));
        when(classificationService.classify(text)).thenReturn(classification);

        // Act
        ClassifyDocumentOutput output = useCase.execute(input);

        // Assert
        assertThat(output).isNotNull();
        assertThat(output.getDocumentId()).isEqualTo(docId.value().toString());
        assertThat(output.getLabel()).isEqualTo("Invoice");
        assertThat(output.getConfidencePercentage()).isEqualTo(88);
        assertThat(output.getModel()).isEqualTo("llama3");

        verify(documentRepository, times(1)).findById(docId);
        verify(classificationService, times(1)).classify(text);
    }

    @Test
    void should_throwException_when_blankText() {
        // Arrange
        DocumentId docId = DocumentId.newId();
        ClassifyDocumentInput input = new ClassifyDocumentInput(docId.value().toString(), "   ");

        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("cannot be blank");
    }

    @Test
    void should_throwException_when_documentNotFound() {
        // Arrange
        DocumentId docId = DocumentId.newId();
        ClassifyDocumentInput input = new ClassifyDocumentInput(docId.value().toString(), "Some text");

        when(documentRepository.findById(docId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void should_throwException_when_nullInput() {
        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(null))
                .isInstanceOf(NullPointerException.class);
    }
}
