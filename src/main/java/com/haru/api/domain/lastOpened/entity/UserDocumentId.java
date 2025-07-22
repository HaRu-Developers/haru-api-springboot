package com.haru.api.domain.lastOpened.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@NoArgsConstructor
public class UserDocumentId implements Serializable {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "document_id")
    private Long documentId;

    public UserDocumentId(Long userId, Long documentId) {
        this.userId = userId;
        this.documentId = documentId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserDocumentId that)) return false;
        return Objects.equals(userId, that.userId) &&
                Objects.equals(documentId, that.documentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, documentId);
    }

}
