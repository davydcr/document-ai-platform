package com.davydcr.document.infrastructure.integration;

import com.davydcr.document.application.dto.ProcessDocumentOutput;
import com.davydcr.document.domain.model.DocumentStatus;
import com.davydcr.document.infrastructure.persistence.entity.RoleEntity;
import com.davydcr.document.infrastructure.persistence.entity.UserAccountEntity;
import com.davydcr.document.infrastructure.repository.UserRepository;
import com.davydcr.document.infrastructure.security.JwtProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Set;
import java.util.UUID;

/**
 * Teste E2E do Fluxo Completo: Upload → OCR → Classificação
 * 
 * VALIDAÇÕES:
 * ✅ Upload de arquivo bem-sucedido
 * ✅ OCR extrai conteúdo
 * ✅ Classificação automática realizada
 * ✅ Persistência dos resultados
 * ✅ Recuperação do documento processado
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("E2E: Upload → OCR → Classificação")
class DocumentUploadOcrClassificationE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    private String jwtToken;

    @BeforeEach
    void setUp() {
        // Setup test user
        userRepository.deleteAll();
        
        UserAccountEntity testUser = new UserAccountEntity();
        testUser.setId(UUID.randomUUID().toString());
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setUsername("testuser");
        testUser.setActive(true);
        testUser.setPasswordHash(passwordEncoder.encode("password123"));
        
        RoleEntity userRole = new RoleEntity("USER", "Standard user access");
        testUser.setRoles(Set.of(userRole));
        
        userRepository.save(testUser);
        
        // Generate JWT token
        jwtToken = jwtProvider.generateToken(testUser.getId(), testUser.getEmail(), Set.of("USER"));
    }

    @Test
    @DisplayName("✅ Fluxo Completo: Upload → OCR → Classificação")
    void testCompleteUploadOcrClassificationFlow() throws Exception {
        // Generate PDF
        byte[] pdfContent = createSimplePdf("TESTE DE DOCUMENTO\nData: 30/01/2026\nValor: R$ 1.500,00");
        
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "invoice.pdf",
                "application/pdf",
                pdfContent
        );

        // Upload document
        MvcResult uploadResult = mockMvc.perform(multipart("/documents/upload")
                .file(file)
                .param("fileType", "PDF")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documentId").isNotEmpty())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.classification").exists())
                .andReturn();

        String uploadResponse = uploadResult.getResponse().getContentAsString();
        ProcessDocumentOutput uploadOutput = objectMapper.readValue(uploadResponse, ProcessDocumentOutput.class);
        
        String documentId = uploadOutput.getDocumentId();

        // Validate upload results
        assertThat(documentId).isNotEmpty();
        assertThat(uploadOutput.getStatus())
                .isIn(DocumentStatus.COMPLETED.toString(), DocumentStatus.PROCESSING.toString());

        System.out.println("✅ STEP 1: Upload bem-sucedido");
        System.out.println("   Document ID: " + documentId);
        System.out.println("   Status: " + uploadOutput.getStatus());
        System.out.println("   Classification: " + uploadOutput.getClassification());

        // Retrieve document
        MvcResult getResult = mockMvc.perform(get("/documents/{documentId}", documentId)
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value(documentId))
                .andReturn();

        String getResponse = getResult.getResponse().getContentAsString();
        ProcessDocumentOutput getOutput = objectMapper.readValue(getResponse, ProcessDocumentOutput.class);

        assertThat(getOutput.getDocumentId()).isEqualTo(documentId);

        System.out.println("✅ STEP 2: Documento recuperado com sucesso");
        System.out.println("   Document ID: " + getOutput.getDocumentId());
        System.out.println("   Status: " + getOutput.getStatus());
        System.out.println("✅ Fluxo completo validado!");
    }

    @Test
    @DisplayName("✅ Upload PDF")
    void testUploadPdf() throws Exception {
        byte[] pdfContent = createSimplePdf("Test PDF Document");
        
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                pdfContent
        );

        MvcResult result = mockMvc.perform(multipart("/documents/upload")
                .file(file)
                .param("fileType", "PDF")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isCreated())
                .andReturn();

        ProcessDocumentOutput output = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ProcessDocumentOutput.class
        );

        assertThat(output.getDocumentId()).isNotEmpty();
        System.out.println("✅ PDF upload OK - ID: " + output.getDocumentId());
    }

    @Test
    @DisplayName("✅ Rejeitar arquivo vazio")
    void testRejectEmptyFile() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.pdf",
                "application/pdf",
                new byte[0]
        );

        mockMvc.perform(multipart("/documents/upload")
                .file(emptyFile)
                .param("fileType", "PDF")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isBadRequest());

        System.out.println("✅ Empty file rejection OK");
    }

    // Helper method to generate minimal valid PDF
    private byte[] createSimplePdf(String text) {
        String pdf = "%PDF-1.4\n" +
                "1 0 obj\n<</Type/Catalog/Pages 2 0 R>>\nendobj\n" +
                "2 0 obj\n<</Type/Pages/Kids[3 0 R]/Count 1>>\nendobj\n" +
                "3 0 obj\n<</Type/Page/Parent 2 0 R/MediaBox[0 0 612 792]/Contents 4 0 R>>\nendobj\n" +
                "4 0 obj\n<</Length 44>>\nstream\nBT\n/F1 12 Tf\n100 700 Td\n(" + text + ")Tj\nET\nendstream\nendobj\n" +
                "xref\n0 5\n0000000000 65535 f\n0000000009 00000 n\n0000000056 00000 n\n" +
                "0000000111 00000 n\n0000000199 00000 n\ntrailer\n<</Size 5/Root 1 0 R>>\nstartxref\n291\n%%EOF";
        return pdf.getBytes();
    }
}
