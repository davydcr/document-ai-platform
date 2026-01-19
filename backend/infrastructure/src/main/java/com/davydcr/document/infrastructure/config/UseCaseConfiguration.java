package com.davydcr.document.infrastructure.config;

import com.davydcr.document.application.port.ClassificationService;
import com.davydcr.document.application.port.DocumentRepository;
import com.davydcr.document.application.port.OcrService;
import com.davydcr.document.application.usecase.ClassifyDocumentUseCase;
import com.davydcr.document.application.usecase.ExtractDocumentContentUseCase;
import com.davydcr.document.application.usecase.GetDocumentUseCase;
import com.davydcr.document.application.usecase.ProcessDocumentUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfiguration {

    @Bean
    public ProcessDocumentUseCase processDocumentUseCase(
            DocumentRepository documentRepository,
            OcrService ocrService,
            ClassificationService classificationService) {
        return new ProcessDocumentUseCase(documentRepository, ocrService, classificationService);
    }

    @Bean
    public ClassifyDocumentUseCase classifyDocumentUseCase(
            DocumentRepository documentRepository,
            ClassificationService classificationService) {
        return new ClassifyDocumentUseCase(documentRepository, classificationService);
    }

    @Bean
    public ExtractDocumentContentUseCase extractDocumentContentUseCase(
            DocumentRepository documentRepository,
            OcrService ocrService) {
        return new ExtractDocumentContentUseCase(documentRepository, ocrService);
    }

    @Bean
    public GetDocumentUseCase getDocumentUseCase(DocumentRepository documentRepository) {
        return new GetDocumentUseCase(documentRepository);
    }
}
