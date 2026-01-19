package com.davydcr.document.application.usecase;

import com.davydcr.document.application.dto.ExtractDocumentContentCommand;
import com.davydcr.document.application.dto.ExtractDocumentContentResult;
import com.davydcr.document.application.port.DocumentRepository;
import com.davydcr.document.application.port.OcrService;
import com.davydcr.document.domain.exception.DomainException;
import com.davydcr.document.domain.model.Document;
import com.davydcr.document.domain.model.ExtractedContent;
import com.davydcr.document.domain.model.ProcessingResult;
import com.davydcr.document.domain.model.ProcessingStatus;

import java.util.Map;
import java.util.Objects;

public class ExtractDocumentContentUseCase {

    private final DocumentRepository documentRepository;
    private final OcrService ocrService;

    public ExtractDocumentContentUseCase(DocumentRepository documentRepository, OcrService ocrService) {
        this.documentRepository = Objects.requireNonNull(documentRepository, "documentRepository cannot be null");
        this.ocrService = Objects.requireNonNull(ocrService, "ocrService cannot be null");
    }

    public ExtractDocumentContentResult execute(ExtractDocumentContentCommand command) {
        Objects.requireNonNull(command, "command cannot be null");

        Document document = documentRepository.findById(command.getDocumentId())
                .orElseThrow(() -> new DomainException("Document not found: " + command.getDocumentId()) {});

        try {
            // Fila o documento se ainda não está processando
            if (document.getStatus().toString().equals("UPLOADED")) {
                document.queue();
            }

            document.startProcessing();

            // Nota: Em produção, aqui seria feito o carregamento do arquivo real
            // Por enquanto, criamos um ExtractedContent simulado
            ExtractedContent extractedContent = new ExtractedContent(
                    "Document content extracted via " + command.getOcrEngine(),
                    1,
                    command.getOcrEngine()
            );

            // Cria resultado com conteúdo extraído
            ProcessingResult result = new ProcessingResult(
                    ProcessingStatus.SUCCESS,
                    Map.of(
                            "pages", 1,
                            "textLength", extractedContent.getFullText().length(),
                            "engine", command.getOcrEngine()
                    ),
                    command.getOcrEngine(),
                    extractedContent,
                    null
            );

            document.finishProcessing(result);
            documentRepository.save(document);

            return new ExtractDocumentContentResult(
                    command.getDocumentId(),
                    1,
                    extractedContent.getFullText().length(),
                    command.getOcrEngine()
            );

        } catch (Exception e) {
            document.startProcessing();
            ProcessingResult errorResult = new ProcessingResult(
                    ProcessingStatus.ERROR,
                    Map.of("error", e.getMessage()),
                    command.getOcrEngine()
            );
            document.finishProcessing(errorResult);
            documentRepository.save(document);

            throw new DomainException("Failed to extract content from document: " + e.getMessage()) {};
        }
    }
}

