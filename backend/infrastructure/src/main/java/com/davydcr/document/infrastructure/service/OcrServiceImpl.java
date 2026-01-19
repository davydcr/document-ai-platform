package com.davydcr.document.infrastructure.service;

import com.davydcr.document.application.port.OcrService;
import com.davydcr.document.domain.model.ExtractedContent;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class OcrServiceImpl implements OcrService {

    @Override
    public ExtractedContent extractContent(InputStream documentStream, String fileType) {
        // Mock implementation - em produção, chamaria Tesseract CLI
        // ou biblioteca como tess4j
        
        String extractedText = "Exemplo de texto extraído de um documento do tipo " + fileType 
                             + " usando OCR Engine. Este é um mock para demonstração.";
        
        return new ExtractedContent(
                extractedText,
                1,  // pageCount mock
                "Tesseract"
        );
    }

    @Override
    public boolean supportsFileType(String fileType) {
        // Suportar tipos comuns
        return fileType != null && (
                fileType.equalsIgnoreCase("pdf") ||
                fileType.equalsIgnoreCase("png") ||
                fileType.equalsIgnoreCase("jpg") ||
                fileType.equalsIgnoreCase("jpeg") ||
                fileType.equalsIgnoreCase("tiff")
        );
    }
}
