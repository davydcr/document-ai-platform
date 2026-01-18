package com.davydcr.document.application.port;

import com.davydcr.document.domain.model.ClassificationLabel;
import com.davydcr.document.domain.model.DocumentClassification;
import com.davydcr.document.domain.model.ExtractedContent;

/**
 * Port para serviço de classificação de documentos.
 * Utiliza LLMs locais (ex: Ollama com llama3, mistral).
 */
public interface ClassificationService {

    DocumentClassification classify(ExtractedContent content);

    DocumentClassification classify(String text);

    String getModelName();

    boolean isAvailable();
}
