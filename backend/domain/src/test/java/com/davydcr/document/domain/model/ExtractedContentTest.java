package com.davydcr.document.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ExtractedContentTest {

    @Test
    void should_create_extractedContent_when_validDataProvided() {
        String text = "This is extracted text";
        int pages = 5;
        String engine = "Tesseract";
        
        ExtractedContent content = new ExtractedContent(text, pages, engine);
        
        assertThat(content.getFullText()).isEqualTo(text);
        assertThat(content.getPageCount()).isEqualTo(pages);
        assertThat(content.getOcrEngine()).isEqualTo(engine);
    }

    @Test
    void should_throwException_when_nullText() {
        assertThatThrownBy(() -> new ExtractedContent(null, 5, "Tesseract"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_throwException_when_nullOcrEngine() {
        assertThatThrownBy(() -> new ExtractedContent("text", 5, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_throwException_when_negativePageCount() {
        assertThatThrownBy(() -> new ExtractedContent("text", -1, "Tesseract"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be negative");
    }

    @Test
    void should_throwException_when_blankOcrEngine() {
        assertThatThrownBy(() -> new ExtractedContent("text", 5, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be blank");
    }

    @Test
    void should_returnTrue_when_hasContent() {
        ExtractedContent contentWithText = new ExtractedContent("Some text", 1, "Tesseract");
        ExtractedContent contentWithoutText = new ExtractedContent("   ", 1, "Tesseract");
        
        assertThat(contentWithText.hasContent()).isTrue();
        assertThat(contentWithoutText.hasContent()).isFalse();
    }

    @Test
    void should_allowZeroPages_when_digitalDocument() {
        ExtractedContent content = new ExtractedContent("text", 0, "Tesseract");
        
        assertThat(content.getPageCount()).isEqualTo(0);
    }

    @Test
    void should_beEqual_when_sameContent() {
        ExtractedContent content1 = new ExtractedContent("text", 5, "Tesseract");
        ExtractedContent content2 = new ExtractedContent("text", 5, "Tesseract");
        
        assertThat(content1).isEqualTo(content2);
        assertThat(content1.hashCode()).isEqualTo(content2.hashCode());
    }
}
