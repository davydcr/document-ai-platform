package com.davydcr.document.infrastructure.controller;

import com.davydcr.document.infrastructure.controller.dto.DocumentDTO;
import com.davydcr.document.infrastructure.persistence.DocumentJpaEntity;
import com.davydcr.document.infrastructure.persistence.DocumentJpaRepository;
import com.davydcr.document.infrastructure.security.SecurityContextService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

/**
 * REST API Controller para consultar Documentos.
 * Endpoints para recuperar e listar documentos (read-only).
 */
@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "*")
public class DocumentQueryController {

    private static final Logger logger = LoggerFactory.getLogger(DocumentQueryController.class);

    private final DocumentJpaRepository documentRepository;
    private final SecurityContextService securityContextService;

    public DocumentQueryController(DocumentJpaRepository documentRepository,
                                 SecurityContextService securityContextService) {
        this.documentRepository = Objects.requireNonNull(documentRepository);
        this.securityContextService = Objects.requireNonNull(securityContextService);
    }

    /**
     * Recupera um documento por ID.
     * GET /api/documents/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<DocumentDTO> getDocument(@PathVariable String id) {

        logger.info("Getting document: id={}", id);

        // Extrair userId do contexto de segurança
        String userId = securityContextService.getCurrentUserId();
        if (userId == null) {
            logger.warn("Unauthorized attempt to get document: id={}", id);
            return ResponseEntity.status(401).build();
        }

        DocumentJpaEntity document = documentRepository.findById(id)
                .orElse(null);

        if (document == null) {
            logger.warn("Document not found: id={}", id);
            return ResponseEntity.notFound().build();
        }

        // Validar ownership (comparar com userId do contexto)
        if (!securityContextService.isOwner(document.getUserId())) {
            logger.warn("Access denied to document: id={}, owner={}, requester={}", 
                       id, document.getUserId(), userId);
            return ResponseEntity.status(403).build();
        }

        DocumentDTO response = mapToDTO(document);

        return ResponseEntity.ok(response);
    }

    /**
     * Lista documentos do usuário com paginação e filtros.
     * GET /api/documents?page=0&size=20&status=COMPLETED&type=PDF
     */
    @GetMapping
    public ResponseEntity<Page<DocumentDTO>> listDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {

        logger.info("Listing documents: page={}, size={}, status={}, type={}", page, size, status, type);

        // Extrair userId do contexto de segurança
        String userId = securityContextService.getCurrentUserId();
        if (userId == null) {
            logger.warn("Unauthorized attempt to list documents");
            return ResponseEntity.status(401).build();
        }

        // Criar pageable com ordenação por createdAt DESC
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // Buscar documentos baseado nos filtros
        Page<DocumentJpaEntity> documents;
        if (status != null && type != null) {
            documents = documentRepository.findByUserIdAndStatusAndType(
                    userId, status, type, pageable);
        } else if (status != null) {
            documents = documentRepository.findByUserIdAndStatus(userId, status, pageable);
        } else if (type != null) {
            documents = documentRepository.findByUserIdAndType(userId, type, pageable);
        } else {
            documents = documentRepository.findByUserId(userId, pageable);
        }

        Page<DocumentDTO> responses = documents.map(this::mapToDTO);

        return ResponseEntity.ok(responses);
    }

    /**
     * Converte entidade para DTO.
     */
    private DocumentDTO mapToDTO(DocumentJpaEntity entity) {
        return new DocumentDTO(
                entity.getId(),
                entity.getOriginalName(),
                entity.getType().toString(),
                entity.getStatus().toString(),
                entity.getExtractedText(),
                entity.getClassificationLabel(),
                entity.getClassificationConfidence(),
                entity.getErrorMessage(),
                entity.getCreatedAt()
        );
    }
}
