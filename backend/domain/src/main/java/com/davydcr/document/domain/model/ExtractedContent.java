package com.davydcr.document.domain.model;

import java.util.Objects;

public class ExtractedContent {

    private final String fullText;
    private final int pageCount;
    private final String ocrEngine;

    public ExtractedContent(String fullText, int pageCount, String ocrEngine) {
        this.fullText = Objects.requireNonNull(fullText, "fullText cannot be null");
        this.pageCount = pageCount;
        this.ocrEngine = Objects.requireNonNull(ocrEngine, "ocrEngine cannot be null");

        if (pageCount < 0) {
            throw new IllegalArgumentException("pageCount cannot be negative");
        }
        if (ocrEngine.isBlank()) {
            throw new IllegalArgumentException("ocrEngine cannot be blank");
        }
    }

    public String getFullText() {
        return fullText;
    }

    public int getPageCount() {
        return pageCount;
    }

    public String getOcrEngine() {
        return ocrEngine;
    }

    public boolean hasContent() {
        return !fullText.isBlank();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExtractedContent that = (ExtractedContent) o;
        return pageCount == that.pageCount &&
                Objects.equals(fullText, that.fullText) &&
                Objects.equals(ocrEngine, that.ocrEngine);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fullText, pageCount, ocrEngine);
    }

    @Override
    public String toString() {
        return "ExtractedContent{" +
                "pageCount=" + pageCount +
                ", ocrEngine='" + ocrEngine + '\'' +
                ", textLength=" + fullText.length() +
                '}';
    }
}
