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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/documents")
@Tag(name = "Documents", description = "API para gerenciamento de documentos")
public class DocumentController {

    private final ProcessDocumentUseCase processDocumentUseCase;
    private final ClassifyDocumentUseCase classifyDocumentUseCase;
    private final ExtractDocumentContentUseCase extractDocumentContentUseCase;
    private final GetDocumentUseCase getDocumentUseCase;

    public DocumentController(ProcessDocumentUseCase processDocumentUseCase,
                             ClassifyDocumentUseCase classifyDocumentUseCase,
                             ExtractDocumentContentUseCase extractDocumentContentUseCase,
                             GetDocumentUseCase getDocumentUseCase) {
        this.processDocumentUseCase = processDocumentUseCase;
        this.classifyDocumentUseCase = classifyDocumentUseCase;
        this.extractDocumentContentUseCase = extractDocumentContentUseCase;
        this.getDocumentUseCase = getDocumentUseCase;
    }

    @PostMapping("/upload")
    @Operation(summary = "Fazer upload de um novo documento",
        description = "Realiza o upload de um novo documento para processamento. " +
            "Suporta PDF, IMAGE e TXT.",
        tags = {"Documents"})
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Documento enviado com sucesso",
            content = @Content(schema = @Schema(implementation = ProcessDocumentOutput.class))),
        @ApiResponse(responseCode = "400", description = "Tipo de arquivo inválido"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<ProcessDocumentOutput> uploadDocument(
            @Parameter(description = "Nome do arquivo", example = "document.pdf")
            @RequestParam String fileName,
            @Parameter(description = "Tipo do documento (PDF, IMAGE, TXT)", example = "PDF")
            @RequestParam String fileType) {
        
        DocumentId documentId = DocumentId.newId();
        
        ProcessDocumentInput input = new ProcessDocumentInput(
                documentId.value().toString(),
                fileName,
                fileType
        );
        
        ProcessDocumentOutput result = processDocumentUseCase.execute(input);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
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
            @Parameter(description = "Modelo de IA para usar (ex: llama3, gpt4)", example = "llama3")
            @RequestParam(defaultValue = "llama3") String model) {
        
        ClassifyDocumentInput input = new ClassifyDocumentInput(
                documentId,
                model
        );
        
        ClassifyDocumentOutput result = classifyDocumentUseCase.execute(input);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{documentId}/extract")
    @Operation(summary = "Extrair conteúdo de um documento",
        description = "Realiza a extração de conteúdo textual de um documento usando OCR",
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
            @Parameter(description = "Caminho do arquivo", example = "/path/to/document.pdf")
            @RequestParam String filePath,
            @Parameter(description = "Engine OCR (Tesseract, etc)", example = "Tesseract")
            @RequestParam(defaultValue = "Tesseract") String ocrEngine) {
        
        ExtractDocumentContentCommand command = new ExtractDocumentContentCommand(
                new DocumentId(UUID.fromString(documentId)),
                filePath,
                ocrEngine
        );
        
        ExtractDocumentContentResult result = extractDocumentContentUseCase.execute(command);
        return ResponseEntity.ok(result);
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
        
        GetDocumentOutput result = getDocumentUseCase.execute(documentId);
        return ResponseEntity.ok(result);
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
        return ResponseEntity.ok("{\"message\": \"Document listing endpoint\"}");
    }
}
