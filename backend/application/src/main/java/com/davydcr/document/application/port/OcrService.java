package com.davydcr.document.application.port;

import com.davydcr.document.domain.model.ExtractedContent;

import java.io.InputStream;

/**
 * Port para serviço de OCR.
 * Responsável por extrair texto de documentos.
 */
public interface OcrService {

    ExtractedContent extractContent(InputStream documentStream, String fileType);

    boolean supportsFileType(String fileType);
}
