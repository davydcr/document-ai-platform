package com.davydcr.document.application.usecase;

import com.davydcr.document.application.dto.ProcessDocumentInput;
import com.davydcr.document.application.dto.ProcessDocumentOutput;
import com.davydcr.document.application.port.ClassificationService;
import com.davydcr.document.application.port.DocumentRepository;
import com.davydcr.document.application.port.OcrService;
import com.davydcr.document.domain.exception.DomainException;
import com.davydcr.document.domain.model.Document;
import com.davydcr.document.domain.model.DocumentClassification;
import com.davydcr.document.domain.model.DocumentId;
import com.davydcr.document.domain.model.ExtractedContent;
import com.davydcr.document.domain.model.ProcessingResult;
import com.davydcr.document.domain.model.ProcessingStatus;
import com.davydcr.document.domain.model.DocumentType;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 * Use case para processar um documento.
 * Orquestra OCR, classificação e persistência.
 */
public class ProcessDocumentUseCase {

    private final DocumentRepository documentRepository;
    private final OcrService ocrService;
    private final ClassificationService classificationService;

    public ProcessDocumentUseCase(DocumentRepository documentRepository,
                                   OcrService ocrService,
                                   ClassificationService classificationService) {
        this.documentRepository = Objects.requireNonNull(documentRepository, "documentRepository cannot be null");
        this.ocrService = Objects.requireNonNull(ocrService, "ocrService cannot be null");
        this.classificationService = Objects.requireNonNull(classificationService, "classificationService cannot be null");
    }

    /**
     * Executa o processamento completo do documento criando-o primeiro.
     * Este é o ponto de entrada para uploads de arquivo.
     * 1. Cria o documento
     * 2. Extração de conteúdo (OCR)
     * 3. Classificação automática
     * 4. Persistência do resultado
     */
    public ProcessDocumentOutput executeWithDocumentCreation(ProcessDocumentInput input, String originalFileName) {
        Objects.requireNonNull(input, "input cannot be null");
        Objects.requireNonNull(originalFileName, "originalFileName cannot be null");

        try {
            // Criar ID do documento
            DocumentId docId = new DocumentId(java.util.UUID.fromString(input.getDocumentId()));
            
            // Criar novo documento
            Document document = new Document(
                    docId,
                    originalFileName,
                    DocumentType.valueOf(input.getFileType())
            );

            // Salvar documento inicial
            documentRepository.save(document);

            // Prosseguir com processamento
            return processDocument(input, document);

        } catch (DomainException e) {
            throw e;
        } catch (Exception e) {
            throw new DomainException("Error creating and processing document: " + e.getMessage()) {};
        }
    }

    /**
     * Executa o processamento completo do documento:
     * 1. Extração de conteúdo (OCR)
     * 2. Classificação automática
     * 3. Persistência do resultado
     */
    public ProcessDocumentOutput execute(ProcessDocumentInput input) {
        Objects.requireNonNull(input, "input cannot be null");

        try {
            // Recuperar documento (já deve existir no repositório)
            DocumentId docId = new DocumentId(java.util.UUID.fromString(input.getDocumentId()));
            Document document = documentRepository.findById(docId)
                    .orElseThrow(() -> new DomainException("Document not found: " + docId) {});

            return processDocument(input, document);

        } catch (DomainException e) {
            throw e;
        } catch (Exception e) {
            throw new DomainException("Error processing document: " + e.getMessage()) {};
        }
    }

    private ProcessDocumentOutput processDocument(ProcessDocumentInput input, Document document) {
        try {
            // Transição de estado
            document.requestProcessing();

            // Extrair conteúdo via OCR
            ExtractedContent extractedContent = performOcr(input.getFilePath(), input.getFileType());

            // Classificar documento
            DocumentClassification classification = classificationService.classify(extractedContent);

            // Criar resultado de processamento
            ProcessingResult result = new ProcessingResult(
                    ProcessingStatus.SUCCESS,
                    Map.of(
                            "extracted_pages", extractedContent.getPageCount(),
                            "ocr_engine", extractedContent.getOcrEngine(),
                            "text_length", extractedContent.getFullText().length()
                    ),
                    classificationService.getModelName(),
                    extractedContent,
                    classification
            );

            // Finalizar processamento no domínio
            document.completeProcessing(result);

            // Persistir documento atualizado
            documentRepository.save(document);

            // Retornar output
            return new ProcessDocumentOutput(
                    input.getDocumentId(),
                    document.getStatus().name(),
                    extractedContent.getFullText().substring(0, Math.min(100, extractedContent.getFullText().length())) + "...",
                    classification.getLabel().getValue(),
                    classification.getConfidence().getPercentage()
            );

        } catch (DomainException e) {
            throw e;
        } catch (Exception e) {
            throw new DomainException("Error processing document: " + e.getMessage()) {};
        }
    }

    private ExtractedContent performOcr(String filePath, String fileType) {
        if (!ocrService.supportsFileType(fileType)) {
            throw new DomainException("File type not supported for OCR: " + fileType) {};
        }

        try (FileInputStream fis = new FileInputStream(filePath)) {
            return ocrService.extractContent(fis, fileType);
        } catch (IOException e) {
            throw new DomainException("Error reading file: " + e.getMessage()) {};
        }
    }
}
