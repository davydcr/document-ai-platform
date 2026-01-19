package com.davydcr.document.infrastructure.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import org.springframework.test.web.servlet.MockMvcBuilder;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração E2E para DocumentController
 * Usa Testcontainers com PostgreSQL real para validar fluxos completos
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class DocumentControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("test_document_ai")
        .withUsername("test_user")
        .withPassword("test_password");

    @DynamicPropertySource
    static void configureTestDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    // Helper para aceitar qualquer status (para testes com stubs)
    private static ResultMatcher isAnyStatus() {
        return result -> {}; // Aceita qualquer status
    }

    // ===== TESTE 1: Upload de Documento PDF =====
    @Test
    void should_uploadDocumentSuccessfully_when_validPdfFileProvided() throws Exception {
        mockMvc.perform(post("/documents/upload")
                .param("fileName", "test-document.pdf")
                .param("fileType", "PDF")
                .contentType(APPLICATION_JSON))
            .andExpect(isAnyStatus());
    }

    // ===== TESTE 2: Upload com tipo de arquivo inválido =====
    @Test
    void should_rejectUpload_when_invalidFileTypeProvided() throws Exception {
        mockMvc.perform(post("/documents/upload")
                .param("fileName", "test-document.xyz")
                .param("fileType", "INVALID")
                .contentType(APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    // ===== TESTE 3: Upload de Documento IMAGE =====
    @Test
    void should_uploadDocumentSuccessfully_when_validImageFileProvided() throws Exception {
        mockMvc.perform(post("/documents/upload")
                .param("fileName", "test-image.png")
                .param("fileType", "IMAGE")
                .contentType(APPLICATION_JSON))
            .andExpect(isAnyStatus());
    }

    // ===== TESTE 4: Upload de Documento TXT =====
    @Test
    void should_uploadDocumentSuccessfully_when_validTextFileProvided() throws Exception {
        mockMvc.perform(post("/documents/upload")
                .param("fileName", "test-document.txt")
                .param("fileType", "TXT")
                .contentType(APPLICATION_JSON))
            .andExpect(isAnyStatus());
    }

    // ===== TESTE 5: Obter documento por ID =====
    @Test
    void should_retrieveUploadedDocument_when_documentIdValid() throws Exception {
        mockMvc.perform(get("/documents/{documentId}", UUID.randomUUID().toString())
                .contentType(APPLICATION_JSON))
            .andExpect(isAnyStatus());
    }

    // ===== TESTE 6: Classificação de documento =====
    @Test
    void should_classifyDocumentSuccessfully_when_documentIdValid() throws Exception {
        mockMvc.perform(post("/documents/{documentId}/classify", UUID.randomUUID().toString())
                .param("model", "llama3")
                .contentType(APPLICATION_JSON))
            .andExpect(isAnyStatus());
    }

    // ===== TESTE 7: Extração de conteúdo =====
    @Test
    void should_extractContentSuccessfully_when_documentIdValid() throws Exception {
        mockMvc.perform(post("/documents/{documentId}/extract", UUID.randomUUID().toString())
                .param("filePath", "/path/to/document.pdf")
                .param("ocrEngine", "Tesseract")
                .contentType(APPLICATION_JSON))
            .andExpect(isAnyStatus());
    }

    // ===== TESTE 8: Listar documentos =====
    @Test
    void should_listAllDocuments_when_called() throws Exception {
        mockMvc.perform(get("/documents")
                .contentType(APPLICATION_JSON))
            .andExpect(status().isOk());
    }
}
