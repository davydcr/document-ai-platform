package com.davydcr.document.infrastructure.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Serviço para gerenciar armazenamento de documentos
 * Salva arquivos em diretório local configurável
 */
@Service
public class DocumentStorageService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentStorageService.class);

    private final String uploadDir;
    private final long maxFileSize;

    public DocumentStorageService(
            @Value("${app.document.upload-dir:/var/document-ai/uploads}") String uploadDir,
            @Value("${app.document.max-file-size:52428800}") long maxFileSize) {
        this.uploadDir = uploadDir;
        this.maxFileSize = maxFileSize; // Default 50MB
        
        // Criar diretório se não existir
        try {
            Files.createDirectories(Paths.get(uploadDir));
            logger.info("Document storage directory ready: {}", uploadDir);
        } catch (IOException e) {
            logger.error("Failed to create upload directory: {}", uploadDir, e);
            throw new RuntimeException("Cannot create upload directory: " + uploadDir, e);
        }
    }

    /**
     * Salva um arquivo enviado e retorna seu caminho
     */
    public String saveDocument(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("File size exceeds maximum allowed: " + maxFileSize + " bytes");
        }

        // Gerar nome único para o arquivo
        String originalFileName = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFileName);
        String uniqueFileName = UUID.randomUUID() + fileExtension;
        
        Path filePath = Paths.get(uploadDir, uniqueFileName);
        
        // Salvar arquivo
        Files.copy(file.getInputStream(), filePath);
        
        logger.info("Document saved successfully. Original: {}, Stored as: {}", 
            originalFileName, uniqueFileName);
        
        return filePath.toString();
    }

    /**
     * Retorna a extensão do arquivo (com ponto)
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        
        int lastDot = fileName.lastIndexOf('.');
        return (lastDot > 0) ? fileName.substring(lastDot) : "";
    }

    /**
     * Deleta um documento armazenado
     */
    public void deleteDocument(String filePath) throws IOException {
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("File path cannot be empty");
        }
        
        Path path = Paths.get(filePath);
        
        // Validar que o arquivo está dentro do diretório permitido
        if (!path.normalize().startsWith(Paths.get(uploadDir).normalize())) {
            throw new IllegalArgumentException("Invalid file path: " + filePath);
        }
        
        if (Files.exists(path)) {
            Files.delete(path);
            logger.info("Document deleted: {}", filePath);
        }
    }

    /**
     * Retorna o caminho para um documento
     */
    public String getDocumentPath(String documentId, String fileExtension) {
        return Paths.get(uploadDir, documentId + fileExtension).toString();
    }

    /**
     * Verifica se um arquivo existe
     */
    public boolean documentExists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }

    /**
     * Retorna o tamanho de um arquivo em bytes
     */
    public long getFileSize(String filePath) throws IOException {
        return Files.size(Paths.get(filePath));
    }
}
