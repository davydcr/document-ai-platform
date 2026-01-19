package com.davydcr.document.infrastructure.service;

import com.davydcr.document.application.port.OcrService;
import com.davydcr.document.domain.model.ExtractedContent;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Serviço real de OCR usando Tesseract + PDFBox
 * Extrai texto de PDFs, imagens (PNG, JPG, TIFF) e arquivos de texto
 */
@Service
public class OcrServiceImpl implements OcrService {

    private static final Logger logger = LoggerFactory.getLogger(OcrServiceImpl.class);

    private final Tesseract tesseract;
    private final String tempDir;

    public OcrServiceImpl(@Value("${app.ocr.temp-dir:/tmp/document-ai}") String tempDir,
                         @Value("${app.ocr.tessdata-path:}") String tessdataPath) {
        this.tempDir = tempDir;
        
        // Inicializar Tesseract
        this.tesseract = new Tesseract();
        
        // Se tessdataPath foi fornecido, usar; caso contrário, Tesseract tentará usar padrão
        if (tessdataPath != null && !tessdataPath.isBlank()) {
            this.tesseract.setDatapath(tessdataPath);
            logger.info("Tesseract configured with custom tessdata path: {}", tessdataPath);
        }
        
        // Configurar idioma padrão (português + inglês)
        this.tesseract.setLanguage("por+eng");
        
        // Criar diretório temporário se não existir
        try {
            Files.createDirectories(Paths.get(tempDir));
            logger.info("Temp directory ready: {}", tempDir);
        } catch (IOException e) {
            logger.warn("Failed to create temp directory: {}", tempDir, e);
        }
    }

    @Override
    public ExtractedContent extractContent(InputStream documentStream, String fileType) {
        logger.debug("Starting OCR extraction for file type: {}", fileType);
        
        if (!supportsFileType(fileType)) {
            throw new IllegalArgumentException("File type not supported for OCR: " + fileType);
        }

        try {
            String tempFile = saveTempFile(documentStream);
            String extractedText;
            int pageCount = 1;

            if ("pdf".equalsIgnoreCase(fileType)) {
                ExtractedContent pdfContent = extractFromPdf(tempFile);
                extractedText = pdfContent.getFullText();
                pageCount = pdfContent.getPageCount();
            } else {
                // Para imagens (PNG, JPG, TIFF, etc.)
                extractedText = extractFromImage(tempFile);
            }

            // Cleanup
            Files.deleteIfExists(Paths.get(tempFile));

            logger.info("OCR extraction completed successfully. Pages: {}, TextLength: {}", 
                pageCount, extractedText.length());

            return new ExtractedContent(
                    extractedText,
                    pageCount,
                    "Tesseract 5.x"
            );

        } catch (Exception e) {
            logger.error("Error during OCR extraction", e);
            throw new RuntimeException("Failed to extract content from document: " + e.getMessage(), e);
        }
    }

    /**
     * Extrai texto de um arquivo PDF usando PDFBox (mais rápido e preciso para PDFs nativos)
     */
    private ExtractedContent extractFromPdf(String filePath) throws IOException {
        logger.debug("Extracting text from PDF: {}", filePath);
        
        PDDocument document = null;
        try {
            // Carregar PDF do arquivo
            document = PDDocument.load(new File(filePath));
            
            // Primeiro, tentar extrair texto nativo do PDF
            PDFTextStripper stripper = new PDFTextStripper();
            String nativeText = stripper.getText(document);
            
            // Se o PDF contém texto nativo e ele não é vazio, usar esse
            if (nativeText != null && !nativeText.trim().isEmpty()) {
                logger.debug("Native PDF text found. Length: {}", nativeText.length());
                return new ExtractedContent(
                        nativeText,
                        document.getNumberOfPages(),
                        "PDFBox (native)"
                );
            }
            
            // Se não houver texto nativo (PDF com imagens/scaneado), usar OCR nas imagens
            logger.debug("No native text found. Performing OCR on PDF pages...");
            return extractFromScannedPdf(document);
            
        } catch (IOException e) {
            logger.error("Error reading PDF file: {}", filePath, e);
            throw e;
        } finally {
            if (document != null) {
                try {
                    document.close();
                } catch (IOException e) {
                    logger.warn("Error closing PDF document", e);
                }
            }
        }
    }

    /**
     * Extrai texto de um PDF escaneado (imagens) usando OCR
     */
    private ExtractedContent extractFromScannedPdf(PDDocument document) throws IOException {
        StringBuilder fullText = new StringBuilder();
        int pageCount = document.getNumberOfPages();
        
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        
        for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
            try {
                logger.debug("Processing page {} of {}", pageIndex + 1, pageCount);
                
                // Renderizar página como imagem com 150 DPI
                BufferedImage image = pdfRenderer.renderImage(pageIndex, 1.5f);
                
                // Extrair texto da imagem com OCR
                String pageText = tesseract.doOCR(image);
                fullText.append(pageText).append("\n");
                
            } catch (TesseractException e) {
                logger.warn("OCR failed for page {}: {}", pageIndex + 1, e.getMessage());
                fullText.append("[OCR failed for page ").append(pageIndex + 1).append("]\n");
            }
        }
        
        return new ExtractedContent(
                fullText.toString(),
                pageCount,
                "Tesseract 5.x (scanned PDF)"
        );
    }

    /**
     * Extrai texto de uma imagem (PNG, JPG, TIFF) usando OCR
     */
    private String extractFromImage(String filePath) throws IOException, TesseractException {
        logger.debug("Extracting text from image: {}", filePath);
        
        BufferedImage image = ImageIO.read(new File(filePath));
        
        if (image == null) {
            throw new IOException("Failed to load image: " + filePath);
        }
        
        String extractedText = tesseract.doOCR(image);
        logger.debug("Image OCR completed. Text length: {}", extractedText.length());
        
        return extractedText;
    }

    /**
     * Salva o InputStream em arquivo temporário
     */
    private String saveTempFile(InputStream inputStream) throws IOException {
        String fileName = UUID.randomUUID() + ".tmp";
        Path tempFilePath = Paths.get(tempDir, fileName);
        
        Files.copy(inputStream, tempFilePath);
        logger.debug("Temp file created: {}", tempFilePath);
        
        return tempFilePath.toString();
    }

    @Override
    public boolean supportsFileType(String fileType) {
        if (fileType == null) {
            return false;
        }
        
        String type = fileType.toLowerCase();
        return type.equalsIgnoreCase("pdf") ||
               type.equalsIgnoreCase("image") ||
               type.equalsIgnoreCase("png") ||
               type.equalsIgnoreCase("jpg") ||
               type.equalsIgnoreCase("jpeg") ||
               type.equalsIgnoreCase("tiff") ||
               type.equalsIgnoreCase("bmp") ||
               type.equalsIgnoreCase("gif");
    }
}

