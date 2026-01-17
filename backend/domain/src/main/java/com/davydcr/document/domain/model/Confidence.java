package com.davydcr.document.domain.model;

import java.util.Objects;

public class Confidence {

    private final int percentage;

    private Confidence(int percentage) {
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("Confidence must be between 0 and 100, got: " + percentage);
        }
        this.percentage = percentage;
    }

    public static Confidence of(int percentage) {
        return new Confidence(percentage);
    }

    public int getPercentage() {
        return percentage;
    }

    public boolean isHighConfidence(int threshold) {
        return percentage >= threshold;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Confidence that = (Confidence) o;
        return percentage == that.percentage;
    }

    @Override
    public int hashCode() {
        return Objects.hash(percentage);
    }

    @Override
    public String toString() {
        return percentage + "%";
    }
}
