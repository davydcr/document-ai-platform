package com.davydcr.document.infrastructure.service;

import com.davydcr.document.application.port.ClassificationService;
import com.davydcr.document.domain.model.ClassificationLabel;
import com.davydcr.document.domain.model.Confidence;
import com.davydcr.document.domain.model.DocumentClassification;
import com.davydcr.document.domain.model.ExtractedContent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementação real de ClassificationService usando Ollama LLM.
 * Conecta a um serviço Ollama rodando em localhost:11434 (ou URL configurada).
 */
@Service
@ConditionalOnProperty(
    name = "app.classification.llm-enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class OllamaClassificationService implements ClassificationService {

    private static final Logger logger = LoggerFactory.getLogger(OllamaClassificationService.class);

    private final RestTemplate restTemplate;
    private final String ollamaUrl;
    private final String modelName;
    private final ObjectMapper objectMapper;

    public OllamaClassificationService(
            RestTemplate restTemplate,
            @Value("${app.classification.llm-url:http://localhost:11434}") String ollamaUrl,
            @Value("${app.classification.llm-model:mistral}") String modelName) {
        this.restTemplate = restTemplate;
        this.ollamaUrl = ollamaUrl;
        this.modelName = modelName;
        this.objectMapper = new ObjectMapper();
        logger.info("OllamaClassificationService initialized with URL: {} and model: {}", ollamaUrl, modelName);
    }

    @Override
    public DocumentClassification classify(ExtractedContent content) {
        return classify(content.getFullText());
    }

    @Override
    public DocumentClassification classify(String text) {
        if (text == null || text.isBlank()) {
            logger.warn("Empty text provided for classification");
            return createDefaultClassification("Unknown", 0);
        }

        try {
            logger.debug("Classifying text with length: {} using model: {}", text.length(), modelName);
            
            // Preparar prompt para classificação
            String prompt = buildClassificationPrompt(text);
            
            // Chamar Ollama API
            String classification = callOllamaAPI(prompt);
            
            // Parsear resposta
            return parseClassificationResponse(classification);

        } catch (Exception e) {
            logger.error("Error classifying text with Ollama", e);
            return createDefaultClassification("Unknown", 0);
        }
    }

    /**
     * Constrói prompt otimizado para classificação de documentos
     */
    private String buildClassificationPrompt(String text) {
        return String.format(
            "Classify the following document text into ONE of these categories: " +
            "Invoice, Contract, Receipt, Report, Letter, Email, Other. " +
            "Respond with ONLY: '[CATEGORY]|[confidence_0_to_100]' format. " +
            "Example: 'Invoice|92'\n\nDocument text:\n%s",
            text.substring(0, Math.min(500, text.length()))
        );
    }

    /**
     * Chama a API do Ollama
     */
    private String callOllamaAPI(String prompt) throws Exception {
        String url = ollamaUrl + "/api/generate";
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelName);
        requestBody.put("prompt", prompt);
        requestBody.put("stream", false);
        requestBody.put("temperature", 0.1);  // Temperatura baixa para respostas consistentes

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        logger.debug("Calling Ollama API: {}", url);
        String response = restTemplate.postForObject(url, entity, String.class);
        
        if (response == null) {
            throw new IllegalStateException("Empty response from Ollama API");
        }

        // Extrair 'response' field do JSON
        JsonNode jsonNode = objectMapper.readTree(response);
        String generatedText = jsonNode.get("response").asText();
        
        logger.debug("Ollama response: {}", generatedText);
        return generatedText;
    }

    /**
     * Parseia a resposta do Ollama no formato '[CATEGORY]|[confidence]'
     */
    private DocumentClassification parseClassificationResponse(String response) {
        try {
            // Limpar resposta
            String cleaned = response.trim()
                .replaceAll("\\[", "")
                .replaceAll("\\]", "")
                .split("\\n")[0];  // Pegar primeira linha

            String[] parts = cleaned.split("\\|");
            
            if (parts.length < 2) {
                logger.warn("Invalid Ollama response format: {}", response);
                return createDefaultClassification("Other", 0);
            }

            String category = parts[0].trim();
            int confidence = parseConfidence(parts[1].trim());

            // Validar categoria conhecida
            if (!isValidCategory(category)) {
                category = "Other";
                confidence = Math.max(0, confidence - 10);
            }

            logger.info("Classification result: category={}, confidence={}", category, confidence);
            return createClassification(category, confidence);

        } catch (Exception e) {
            logger.error("Error parsing Ollama response: {}", response, e);
            return createDefaultClassification("Other", 0);
        }
    }

    /**
     * Valida se a categoria é conhecida
     */
    private boolean isValidCategory(String category) {
        return category.equalsIgnoreCase("Invoice") ||
               category.equalsIgnoreCase("Contract") ||
               category.equalsIgnoreCase("Receipt") ||
               category.equalsIgnoreCase("Report") ||
               category.equalsIgnoreCase("Letter") ||
               category.equalsIgnoreCase("Email") ||
               category.equalsIgnoreCase("Other");
    }

    /**
     * Parseia confidence como inteiro (0-100)
     */
    private int parseConfidence(String confidenceStr) {
        try {
            int conf = Integer.parseInt(confidenceStr.replaceAll("[^0-9]", ""));
            return Math.max(0, Math.min(100, conf));  // Limitar entre 0-100
        } catch (NumberFormatException e) {
            logger.warn("Could not parse confidence: {}", confidenceStr);
            return 50;  // Default confidence
        }
    }

    /**
     * Cria classificação com label e confiança
     */
    private DocumentClassification createClassification(String label, int confidence) {
        try {
            ClassificationLabel classificationLabel = ClassificationLabel.of(label);
            Confidence confObject = Confidence.of(confidence);
            return new DocumentClassification(classificationLabel, confObject, getModelName());
        } catch (Exception e) {
            logger.error("Error creating classification object", e);
            return createDefaultClassification("Unknown", 0);
        }
    }

    /**
     * Cria classificação padrão em caso de erro
     */
    private DocumentClassification createDefaultClassification(String label, int confidence) {
        try {
            ClassificationLabel classificationLabel = ClassificationLabel.of(label);
            Confidence confObject = Confidence.of(confidence);
            return new DocumentClassification(classificationLabel, confObject, getModelName());
        } catch (Exception e) {
            logger.error("Error creating default classification", e);
            throw new RuntimeException("Failed to create classification", e);
        }
    }

    @Override
    public String getModelName() {
        return "ollama-" + modelName;
    }

    @Override
    public boolean isAvailable() {
        try {
            String healthUrl = ollamaUrl + "/api/tags";
            restTemplate.getForObject(healthUrl, String.class);
            logger.info("Ollama service is available");
            return true;
        } catch (Exception e) {
            logger.error("Ollama service is not available: {}", e.getMessage());
            return false;
        }
    }
}
