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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/documents")
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
    public ResponseEntity<ProcessDocumentOutput> uploadDocument(
            @RequestParam String fileName,
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
    public ResponseEntity<ClassifyDocumentOutput> classifyDocument(
            @PathVariable String documentId,
            @RequestParam(defaultValue = "llama3") String model) {
        
        ClassifyDocumentInput input = new ClassifyDocumentInput(
                documentId,
                model
        );
        
        ClassifyDocumentOutput result = classifyDocumentUseCase.execute(input);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{documentId}/extract")
    public ResponseEntity<ExtractDocumentContentResult> extractContent(
            @PathVariable String documentId,
            @RequestParam String filePath,
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
    public ResponseEntity<GetDocumentOutput> getDocument(
            @PathVariable String documentId) {
        
        GetDocumentOutput result = getDocumentUseCase.execute(documentId);
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<String> listDocuments() {
        return ResponseEntity.ok("{\"message\": \"Document listing endpoint\"}");
    }
}
