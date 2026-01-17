package com.davydcr.document.domain.model;

import java.util.Objects;

public class ClassificationLabel {

    private final String value;

    private ClassificationLabel(String value) {
        this.value = Objects.requireNonNull(value, "ClassificationLabel cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("ClassificationLabel cannot be blank");
        }
        if (value.length() > 100) {
            throw new IllegalArgumentException("ClassificationLabel cannot exceed 100 characters");
        }
    }

    public static ClassificationLabel of(String value) {
        return new ClassificationLabel(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassificationLabel that = (ClassificationLabel) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
