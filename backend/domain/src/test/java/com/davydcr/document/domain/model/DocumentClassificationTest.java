package com.davydcr.document.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class DocumentClassificationTest {

    @Test
    void should_create_classification_when_validDataProvided() {
        ClassificationLabel label = ClassificationLabel.of("Invoice");
        Confidence confidence = Confidence.of(95);
        String model = "llama3";
        
        DocumentClassification classification = new DocumentClassification(label, confidence, model);
        
        assertThat(classification.getLabel()).isEqualTo(label);
        assertThat(classification.getConfidence()).isEqualTo(confidence);
        assertThat(classification.getModel()).isEqualTo(model);
        assertThat(classification.getClassifiedAt()).isNotNull();
    }

    @Test
    void should_throwException_when_nullLabel() {
        Confidence confidence = Confidence.of(95);
        
        assertThatThrownBy(() -> new DocumentClassification(null, confidence, "model"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_throwException_when_nullConfidence() {
        ClassificationLabel label = ClassificationLabel.of("Invoice");
        
        assertThatThrownBy(() -> new DocumentClassification(label, null, "model"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_throwException_when_nullModel() {
        ClassificationLabel label = ClassificationLabel.of("Invoice");
        Confidence confidence = Confidence.of(95);
        
        assertThatThrownBy(() -> new DocumentClassification(label, confidence, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_throwException_when_blankModel() {
        ClassificationLabel label = ClassificationLabel.of("Invoice");
        Confidence confidence = Confidence.of(95);
        
        assertThatThrownBy(() -> new DocumentClassification(label, confidence, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be blank");
    }

    @Test
    void should_returnTrue_when_isReliableAboveThreshold() {
        ClassificationLabel label = ClassificationLabel.of("Invoice");
        Confidence confidence = Confidence.of(85);
        DocumentClassification classification = new DocumentClassification(label, confidence, "model");
        
        assertThat(classification.isReliable(80)).isTrue();
        assertThat(classification.isReliable(90)).isFalse();
    }

    @Test
    void should_beEqual_when_sameData() {
        ClassificationLabel label = ClassificationLabel.of("Invoice");
        Confidence confidence = Confidence.of(95);
        DocumentClassification classification = new DocumentClassification(label, confidence, "model");
        
        assertThat(classification.getLabel()).isEqualTo(label);
        assertThat(classification.getConfidence()).isEqualTo(confidence);
        assertThat(classification.getModel()).isEqualTo("model");
    }
}
