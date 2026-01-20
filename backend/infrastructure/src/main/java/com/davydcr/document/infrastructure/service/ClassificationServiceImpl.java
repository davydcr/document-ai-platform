package com.davydcr.document.infrastructure.service;

import com.davydcr.document.application.port.ClassificationService;
import com.davydcr.document.domain.model.ClassificationLabel;
import com.davydcr.document.domain.model.Confidence;
import com.davydcr.document.domain.model.DocumentClassification;
import com.davydcr.document.domain.model.ExtractedContent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementação mock de ClassificationService (fallback).
 * Usado quando app.classification.llm-enabled=false ou Ollama não está disponível.
 */
@Service
@ConditionalOnProperty(
    name = "app.classification.llm-enabled",
    havingValue = "false",
    matchIfMissing = true
)
public class ClassificationServiceImpl implements ClassificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ClassificationServiceImpl.class);

    @Override
    public DocumentClassification classify(ExtractedContent content) {
        return classify(content.getFullText());
    }

    @Override
    public DocumentClassification classify(String text) {
        logger.debug("Using mock classification (LLM not enabled)");
        
        ClassificationLabel label;
        Confidence confidence;

        // Lógica simplificada para mock
        if (text.toLowerCase().contains("invoice") || 
            text.toLowerCase().contains("fatura")) {
            label = ClassificationLabel.of("Invoice");
            confidence = Confidence.of(95);
        } else if (text.toLowerCase().contains("contract") || 
                   text.toLowerCase().contains("contrato")) {
            label = ClassificationLabel.of("Contract");
            confidence = Confidence.of(90);
        } else if (text.toLowerCase().contains("receipt") || 
                   text.toLowerCase().contains("recibo")) {
            label = ClassificationLabel.of("Receipt");
            confidence = Confidence.of(88);
        } else {
            label = ClassificationLabel.of("Other");
            confidence = Confidence.of(75);
        }

        return new DocumentClassification(label, confidence, getModelName());
    }

    @Override
    public String getModelName() {
        return "mock-llm";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
