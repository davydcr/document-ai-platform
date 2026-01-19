package com.davydcr.document.infrastructure.controller;

import com.davydcr.document.application.dto.ClassifyDocumentInput;
import com.davydcr.document.application.dto.ClassifyDocumentOutput;
import com.davydcr.document.application.dto.ExtractDocumentContentCommand;
import com.davydcr.document.application.dto.ExtractDocumentContentResult;
import com.davydcr.document.application.dto.GetDocumentOutput;
import com.davydcr.document.application.dto.ProcessDocumentInput;
import com.davydcr.document.application.dto.ProcessDocumentOutput;
import com.davydcr.document.application.usecase.ClassifyDocumentUseCase;
import com.davydcr.document.application.usecase.ExtractDocumentContentUseCase;
import com.davydcr.document.application.usecase.GetDocumentUseCase;
import com.davydcr.document.application.usecase.ProcessDocumentUseCase;
import com.davydcr.document.domain.model.DocumentId;
import com.davydcr.document.infrastructure.service.DocumentStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/documents")
@Tag(name = "Documents", description = "API para gerenciamento de documentos")
public class DocumentController {

    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);

    private final ProcessDocumentUseCase processDocumentUseCase;
    private final ClassifyDocumentUseCase classifyDocumentUseCase;
    private final ExtractDocumentContentUseCase extractDocumentContentUseCase;
    private final GetDocumentUseCase getDocumentUseCase;
    private final DocumentStorageService storageService;

    public DocumentController(ProcessDocumentUseCase processDocumentUseCase,
                             ClassifyDocumentUseCase classifyDocumentUseCase,
                             ExtractDocumentContentUseCase extractDocumentContentUseCase,
                             GetDocumentUseCase getDocumentUseCase,
                             DocumentStorageService storageService) {
        this.processDocumentUseCase = processDocumentUseCase;
        this.classifyDocumentUseCase = classifyDocumentUseCase;
        this.extractDocumentContentUseCase = extractDocumentContentUseCase;
        this.getDocumentUseCase = getDocumentUseCase;
        this.storageService = storageService;
    }

    @PostMapping("/upload")
    @Operation(summary = "Fazer upload de um novo documento",
        description = "Realiza o upload de um novo documento para processamento com OCR real. " +
            "Suporta PDF, IMAGE e TXT. Máximo 50MB.",
        tags = {"Documents"})
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Documento enviado com sucesso e processado",
            content = @Content(schema = @Schema(implementation = ProcessDocumentOutput.class))),
        @ApiResponse(responseCode = "400", description = "Tipo de arquivo inválido ou arquivo vazio"),
        @ApiResponse(responseCode = "413", description = "Arquivo excede tamanho máximo"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<ProcessDocumentOutput> uploadDocument(
            @Parameter(description = "Arquivo do documento", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Tipo do documento (PDF, IMAGE, TXT)", example = "PDF")
            @RequestParam(defaultValue = "PDF") String fileType) {
        
        try {
            logger.info("Document upload started: fileName={}, fileType={}, size={}", 
                file.getOriginalFilename(), fileType, file.getSize());
            
            if (file.isEmpty()) {
                logger.warn("Upload attempted with empty file");
                return ResponseEntity.badRequest().build();
            }

            // Determinar tipo de arquivo
            String detectedType = detectFileType(file.getOriginalFilename(), fileType);
            
            // Criar ID do documento
            DocumentId documentId = DocumentId.newId();
            
            // Salvar arquivo
            String filePath = storageService.saveDocument(file);
            logger.info("Document saved to: {} with ID: {}", filePath, documentId.value());
            
            // Processar documento com OCR real (cria e processa)
            ProcessDocumentInput input = new ProcessDocumentInput(
                    documentId.value().toString(),
                    filePath,
                    detectedType
            );
            
            ProcessDocumentOutput result = processDocumentUseCase.executeWithDocumentCreation(input, file.getOriginalFilename());
            logger.info("Document processed successfully: {}", documentId.value());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid document upload: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error processing document upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{documentId}/classify")
    @Operation(summary = "Classificar um documento",
        description = "Realiza a classificação automática de um documento usando modelo de IA",
        tags = {"Documents"})
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Documento classificado com sucesso",
            content = @Content(schema = @Schema(implementation = ClassifyDocumentOutput.class))),
        @ApiResponse(responseCode = "404", description = "Documento não encontrado"),
        @ApiResponse(responseCode = "500", description = "Erro na classificação")
    })
    public ResponseEntity<ClassifyDocumentOutput> classifyDocument(
            @Parameter(description = "ID do documento", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String documentId,
            @Parameter(description = "Texto a classificar (opcional, usa conteúdo extraído se não fornecido)")
            @RequestParam(required = false) String text,
            @Parameter(description = "Modelo de IA para usar (ex: llama3, gpt4)", example = "llama3")
            @RequestParam(defaultValue = "llama3") String model) {
        
        try {
            logger.debug("Classification request for document: {}", documentId);
            
            ClassifyDocumentInput input = new ClassifyDocumentInput(
                    documentId,
                    text != null ? text : ""
            );
            
            ClassifyDocumentOutput result = classifyDocumentUseCase.execute(input);
            logger.info("Document classified successfully: {}", documentId);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error classifying document: {}", documentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{documentId}/extract")
    @Operation(summary = "Extrair conteúdo de um documento com OCR",
        description = "Realiza a extração de conteúdo textual de um documento usando OCR real (Tesseract)",
        tags = {"Documents"})
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Conteúdo extraído com sucesso",
            content = @Content(schema = @Schema(implementation = ExtractDocumentContentResult.class))),
        @ApiResponse(responseCode = "404", description = "Documento não encontrado"),
        @ApiResponse(responseCode = "500", description = "Erro na extração")
    })
    public ResponseEntity<ExtractDocumentContentResult> extractContent(
            @Parameter(description = "ID do documento", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String documentId,
            @Parameter(description = "Caminho do arquivo", example = "/var/document-ai/uploads/abc123.pdf")
            @RequestParam String filePath,
            @Parameter(description = "Engine OCR (Tesseract, PaddleOCR, etc)", example = "Tesseract")
            @RequestParam(defaultValue = "Tesseract") String ocrEngine) {
        
        try {
            logger.info("OCR extraction requested for document: {} using engine: {}", documentId, ocrEngine);
            
            ExtractDocumentContentCommand command = new ExtractDocumentContentCommand(
                    new DocumentId(UUID.fromString(documentId)),
                    filePath,
                    ocrEngine
            );
            
            ExtractDocumentContentResult result = extractDocumentContentUseCase.execute(command);
            logger.info("OCR extraction completed for document: {} - {} pages, {} chars", 
                documentId, result.getPageCount(), result.getTextLength());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error extracting document content: {}", documentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{documentId}")
    @Operation(summary = "Obter um documento por ID",
        description = "Recupera as informações completas de um documento pelo seu ID",
        tags = {"Documents"})
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Documento encontrado",
            content = @Content(schema = @Schema(implementation = GetDocumentOutput.class))),
        @ApiResponse(responseCode = "404", description = "Documento não encontrado"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<GetDocumentOutput> getDocument(
            @Parameter(description = "ID do documento", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String documentId) {
        
        try {
            logger.debug("Fetching document: {}", documentId);
            
            GetDocumentOutput result = getDocumentUseCase.execute(documentId);
            logger.info("Document retrieved: {}", documentId);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error retrieving document: {}", documentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    @Operation(summary = "Listar todos os documentos",
        description = "Retorna a lista de todos os documentos processados",
        tags = {"Documents"})
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de documentos retornada com sucesso"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<String> listDocuments() {
        try {
            logger.debug("Listing all documents");
            return ResponseEntity.ok("{\"message\": \"Document listing endpoint\"}");
        } catch (Exception e) {
            logger.error("Error listing documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Detecta o tipo de arquivo com base na extensão ou no parâmetro fornecido
     */
    private String detectFileType(String fileName, String providedType) {
        if (fileName == null) {
            return providedType;
        }

        String lowerFileName = fileName.toLowerCase();
        
        if (lowerFileName.endsWith(".pdf")) {
            return "PDF";
        } else if (lowerFileName.endsWith(".txt")) {
            return "TXT";
        } else if (lowerFileName.endsWith(".png") || 
                   lowerFileName.endsWith(".jpg") || 
                   lowerFileName.endsWith(".jpeg") || 
                   lowerFileName.endsWith(".tiff") ||
                   lowerFileName.endsWith(".bmp") ||
                   lowerFileName.endsWith(".gif")) {
            return "IMAGE";
        }
        
        return providedType;
    }
}

