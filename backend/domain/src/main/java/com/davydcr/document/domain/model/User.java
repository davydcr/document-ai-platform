package com.davydcr.document.domain.model;

import com.davydcr.document.domain.exception.DomainException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class User {

    private static final Pattern EMAIL_PATTERN = 
            Pattern.compile("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");

    private final UserId id;
    private final String name;
    private final String email;
    private final Instant createdAt;
    private final List<DocumentId> documentIds = new ArrayList<>();

    public User(UserId id, String name, String email) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.email = Objects.requireNonNull(email, "email cannot be null");

        validateName(name);
        validateEmail(email);

        this.createdAt = Instant.now();
    }

    private void validateName(String name) {
        if (name.isBlank()) {
            throw new DomainException("User name cannot be blank") {};
        }
        if (name.length() < 2 || name.length() > 150) {
            throw new DomainException("User name must be between 2 and 150 characters") {};
        }
    }

    private void validateEmail(String email) {
        if (email.isBlank()) {
            throw new DomainException("User email cannot be blank") {};
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new DomainException("User email is invalid: " + email) {};
        }
    }

    public void addDocument(DocumentId documentId) {
        Objects.requireNonNull(documentId, "documentId cannot be null");
        if (documentIds.contains(documentId)) {
            throw new DomainException("Document already owned by this user: " + documentId) {};
        }
        documentIds.add(documentId);
    }

    public void removeDocument(DocumentId documentId) {
        if (!documentIds.contains(documentId)) {
            throw new DomainException("Document not found for this user: " + documentId) {};
        }
        documentIds.remove(documentId);
    }

    public UserId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<DocumentId> getDocuments() {
        return Collections.unmodifiableList(documentIds);
    }

    public int getDocumentCount() {
        return documentIds.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", documentCount=" + documentIds.size() +
                '}';
    }
}
