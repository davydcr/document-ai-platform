package com.davydcr.document.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class UserIdTest {

    @Test
    void should_create_userId_when_validStringProvided() {
        String value = "user-123";
        
        UserId userId = UserId.of(value);
        
        assertThat(userId.getValue()).isEqualTo(value);
    }

    @Test
    void should_create_newId_when_newIdCalled() {
        UserId userId1 = UserId.newId();
        UserId userId2 = UserId.newId();
        
        assertThat(userId1.getValue()).isNotBlank();
        assertThat(userId2.getValue()).isNotBlank();
        assertThat(userId1).isNotEqualTo(userId2);
    }

    @Test
    void should_throwException_when_nullValueProvided() {
        assertThatThrownBy(() -> UserId.of(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_throwException_when_blankValueProvided() {
        assertThatThrownBy(() -> UserId.of("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be blank");
    }

    @Test
    void should_beEqual_when_sameValue() {
        UserId userId1 = UserId.of("user-123");
        UserId userId2 = UserId.of("user-123");
        
        assertThat(userId1).isEqualTo(userId2);
        assertThat(userId1.hashCode()).isEqualTo(userId2.hashCode());
    }
}
