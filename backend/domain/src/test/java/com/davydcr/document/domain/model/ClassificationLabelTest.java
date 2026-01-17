package com.davydcr.document.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ClassificationLabelTest {

    @Test
    void should_create_label_when_validStringProvided() {
        ClassificationLabel label = ClassificationLabel.of("Invoice");
        
        assertThat(label.getValue()).isEqualTo("Invoice");
    }

    @Test
    void should_throwException_when_nullValueProvided() {
        assertThatThrownBy(() -> ClassificationLabel.of(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_throwException_when_blankValueProvided() {
        assertThatThrownBy(() -> ClassificationLabel.of("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be blank");
    }

    @Test
    void should_throwException_when_labelExceedsMaxLength() {
        String longLabel = "a".repeat(101);
        
        assertThatThrownBy(() -> ClassificationLabel.of(longLabel))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot exceed 100 characters");
    }

    @Test
    void should_allowMaxLength_when_exactlyHundredCharacters() {
        String maxLabel = "a".repeat(100);
        
        ClassificationLabel label = ClassificationLabel.of(maxLabel);
        
        assertThat(label.getValue()).isEqualTo(maxLabel);
        assertThat(label.getValue().length()).isEqualTo(100);
    }

    @Test
    void should_beEqual_when_sameValue() {
        ClassificationLabel label1 = ClassificationLabel.of("Invoice");
        ClassificationLabel label2 = ClassificationLabel.of("Invoice");
        
        assertThat(label1).isEqualTo(label2);
        assertThat(label1.hashCode()).isEqualTo(label2.hashCode());
    }
}
