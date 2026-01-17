package com.davydcr.document.domain.model;

import com.davydcr.document.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class UserTest {

    @Test
    void should_create_user_when_validDataProvided() {
        UserId userId = UserId.newId();
        String name = "João Silva";
        String email = "joao@example.com";
        
        User user = new User(userId, name, email);
        
        assertThat(user.getId()).isEqualTo(userId);
        assertThat(user.getName()).isEqualTo(name);
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getDocumentCount()).isZero();
    }

    @Test
    void should_throwException_when_nullId() {
        assertThatThrownBy(() -> new User(null, "João", "joao@example.com"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_throwException_when_nullName() {
        UserId userId = UserId.newId();
        
        assertThatThrownBy(() -> new User(userId, null, "joao@example.com"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_throwException_when_nullEmail() {
        UserId userId = UserId.newId();
        
        assertThatThrownBy(() -> new User(userId, "João", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_throwException_when_blankName() {
        UserId userId = UserId.newId();
        
        assertThatThrownBy(() -> new User(userId, "   ", "joao@example.com"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("cannot be blank");
    }

    @Test
    void should_throwException_when_nameTooShort() {
        UserId userId = UserId.newId();
        
        assertThatThrownBy(() -> new User(userId, "J", "joao@example.com"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("between 2 and 150");
    }

    @Test
    void should_throwException_when_nameTooLong() {
        UserId userId = UserId.newId();
        String longName = "a".repeat(151);
        
        assertThatThrownBy(() -> new User(userId, longName, "joao@example.com"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("between 2 and 150");
    }

    @Test
    void should_throwException_when_invalidEmailFormat() {
        UserId userId = UserId.newId();
        
        assertThatThrownBy(() -> new User(userId, "João", "invalid-email"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("invalid");
        
        assertThatThrownBy(() -> new User(userId, "João", "user@"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("invalid");
    }

    @Test
    void should_throwException_when_blankEmail() {
        UserId userId = UserId.newId();
        
        assertThatThrownBy(() -> new User(userId, "João", "   "))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("cannot be blank");
    }

    @Test
    void should_addDocument_when_validDocumentId() {
        UserId userId = UserId.newId();
        User user = new User(userId, "João", "joao@example.com");
        DocumentId docId = DocumentId.newId();
        
        user.addDocument(docId);
        
        assertThat(user.getDocumentCount()).isOne();
        assertThat(user.getDocuments()).contains(docId);
    }

    @Test
    void should_throwException_when_addingDuplicateDocument() {
        UserId userId = UserId.newId();
        User user = new User(userId, "João", "joao@example.com");
        DocumentId docId = DocumentId.newId();
        
        user.addDocument(docId);
        
        assertThatThrownBy(() -> user.addDocument(docId))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("already owned");
    }

    @Test
    void should_throwException_when_addingNullDocument() {
        UserId userId = UserId.newId();
        User user = new User(userId, "João", "joao@example.com");
        
        assertThatThrownBy(() -> user.addDocument(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_removeDocument_when_documentOwned() {
        UserId userId = UserId.newId();
        User user = new User(userId, "João", "joao@example.com");
        DocumentId docId = DocumentId.newId();
        
        user.addDocument(docId);
        user.removeDocument(docId);
        
        assertThat(user.getDocumentCount()).isZero();
        assertThat(user.getDocuments()).doesNotContain(docId);
    }

    @Test
    void should_throwException_when_removingUnownedDocument() {
        UserId userId = UserId.newId();
        User user = new User(userId, "João", "joao@example.com");
        DocumentId docId = DocumentId.newId();
        
        assertThatThrownBy(() -> user.removeDocument(docId))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void should_beEqual_when_sameId() {
        UserId userId = UserId.of("same-id");
        User user1 = new User(userId, "João", "joao@example.com");
        User user2 = new User(userId, "Maria", "maria@example.com");
        
        assertThat(user1).isEqualTo(user2);
    }

    @Test
    void should_acceptValidEmailFormats() {
        UserId userId = UserId.newId();
        
        User user1 = new User(userId, "João", "joao+tag@example.com");
        User user2 = new User(UserId.newId(), "João", "joao.silva@sub.example.co.uk");
        
        assertThat(user1.getEmail()).isEqualTo("joao+tag@example.com");
        assertThat(user2.getEmail()).isEqualTo("joao.silva@sub.example.co.uk");
    }
}
