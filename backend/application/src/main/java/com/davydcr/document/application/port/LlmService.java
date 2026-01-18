package com.davydcr.document.application.port;

import java.util.Map;

/**
 * Port para serviço de IA/LLM.
 * Realiza análise semântica e processamento via LLMs locais.
 */
public interface LlmService {

    String analyze(String text, String prompt);

    Map<String, Object> extractMetadata(String text);

    String summarize(String text, int maxTokens);

    String getModelName();

    boolean isAvailable();
}
