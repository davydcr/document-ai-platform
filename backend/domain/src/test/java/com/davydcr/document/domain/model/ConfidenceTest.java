package com.davydcr.document.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ConfidenceTest {

    @Test
    void should_create_confidence_when_validPercentageProvided() {
        Confidence confidence = Confidence.of(75);
        
        assertThat(confidence.getPercentage()).isEqualTo(75);
    }

    @Test
    void should_allowZeroAndHundred_when_boundaryValues() {
        Confidence zero = Confidence.of(0);
        Confidence hundred = Confidence.of(100);
        
        assertThat(zero.getPercentage()).isEqualTo(0);
        assertThat(hundred.getPercentage()).isEqualTo(100);
    }

    @Test
    void should_throwException_when_negativePercentage() {
        assertThatThrownBy(() -> Confidence.of(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 0 and 100");
    }

    @Test
    void should_throwException_when_percentageGreaterThanHundred() {
        assertThatThrownBy(() -> Confidence.of(101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 0 and 100");
    }

    @Test
    void should_returnTrue_when_isHighConfidenceAboveThreshold() {
        Confidence confidence = Confidence.of(85);
        
        assertThat(confidence.isHighConfidence(80)).isTrue();
        assertThat(confidence.isHighConfidence(85)).isTrue();
        assertThat(confidence.isHighConfidence(90)).isFalse();
    }

    @Test
    void should_beEqual_when_samePercentage() {
        Confidence conf1 = Confidence.of(75);
        Confidence conf2 = Confidence.of(75);
        
        assertThat(conf1).isEqualTo(conf2);
        assertThat(conf1.hashCode()).isEqualTo(conf2.hashCode());
    }

    @Test
    void should_formatToString_withPercentageSymbol() {
        Confidence confidence = Confidence.of(85);
        
        assertThat(confidence.toString()).isEqualTo("85%");
    }
}
