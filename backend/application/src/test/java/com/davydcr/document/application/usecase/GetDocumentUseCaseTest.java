package com.davydcr.document.application.usecase;

import com.davydcr.document.application.dto.GetDocumentOutput;
import com.davydcr.document.application.port.DocumentRepository;
import com.davydcr.document.domain.exception.DomainException;
import com.davydcr.document.domain.model.Document;
import com.davydcr.document.domain.model.DocumentId;
import com.davydcr.document.domain.model.DocumentType;
import com.davydcr.document.domain.model.ProcessingResult;
import com.davydcr.document.domain.model.ProcessingStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetDocumentUseCaseTest {

    @Mock
    private DocumentRepository documentRepository;

    private GetDocumentUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetDocumentUseCase(documentRepository);
    }

    @Test
    void should_getDocumentSuccessfully_when_documentExists() {
        // Arrange
        DocumentId docId = DocumentId.newId();
        Document document = new Document(docId, "invoice.pdf", DocumentType.PDF);
        
        document.requestProcessing();
        document.clearDomainEvents();
        document.completeProcessing(new ProcessingResult(
                ProcessingStatus.SUCCESS,
                Map.of(),
                "model-v1"
        ));

        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));

        // Act
        GetDocumentOutput output = useCase.execute(docId.value().toString());

        // Assert
        assertThat(output).isNotNull();
        assertThat(output.getDocumentId()).isEqualTo(docId.value().toString());
        assertThat(output.getOriginalName()).isEqualTo("invoice.pdf");
        assertThat(output.getType()).isEqualTo("PDF");
        assertThat(output.getStatus()).isEqualTo("COMPLETED");
        assertThat(output.getTotalProcessingAttempts()).isEqualTo(1);
        assertThat(output.getSuccessfulProcessing()).isEqualTo(1);

        verify(documentRepository, times(1)).findById(docId);
    }

    @Test
    void should_countProcessingAttempts_correctly() {
        // Arrange
        DocumentId docId = DocumentId.newId();
        Document document = new Document(docId, "document.pdf", DocumentType.PDF);

        // Simulate multiple processing attempts
        document.requestProcessing();
        document.clearDomainEvents();
        document.completeProcessing(new ProcessingResult(ProcessingStatus.SUCCESS, Map.of(), "v1"));

        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));

        // Act
        GetDocumentOutput output = useCase.execute(docId.value().toString());

        // Assert
        assertThat(output.getTotalProcessingAttempts()).isEqualTo(1);
        assertThat(output.getSuccessfulProcessing()).isEqualTo(1);
    }

    @Test
    void should_throwException_when_documentNotFound() {
        // Arrange
        DocumentId docId = DocumentId.newId();

        when(documentRepository.findById(docId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(docId.value().toString()))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void should_throwException_when_nullDocumentId() {
        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(null))
                .isInstanceOf(NullPointerException.class);
    }
}
