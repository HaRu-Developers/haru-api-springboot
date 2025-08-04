package com.haru.api.domain.lastOpened.entity;

import com.haru.api.domain.lastOpened.entity.enums.DocumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@NoArgsConstructor
public class UserDocumentId implements Serializable {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "document_id")
    @Getter
    private Long documentId;

    @Column(name = "document_type")
    @Enumerated(EnumType.STRING)
    private DocumentType documentType;


    public UserDocumentId(Long userId, Long documentId, DocumentType documentType) {
        this.userId = userId;
        this.documentId = documentId;
        this.documentType = documentType;
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
