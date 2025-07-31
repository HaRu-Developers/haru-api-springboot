package com.haru.api.domain.lastOpened.entity;

import com.haru.api.domain.lastOpened.entity.enums.DocumentType;
import com.haru.api.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_document_last_opened")
@Getter
@DynamicUpdate
@DynamicInsert
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserDocumentLastOpened {

    @EmbeddedId
    private UserDocumentId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_user_document_user"))
    private User user;

    @Column(name = "last_opened")
    private LocalDateTime lastOpened;

}
