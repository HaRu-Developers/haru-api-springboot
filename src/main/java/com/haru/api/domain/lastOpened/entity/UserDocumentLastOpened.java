package com.haru.api.domain.lastOpened.entity;

import com.haru.api.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_document_last_opened",
    indexes = {
        @Index(name = "idx_workspace_user_last_opened",
               columnList = "workspace_id, user_id, last_opened DESC")
})
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

    @Column(name = "title")
    private String title;

    @Column(name = "workspace_id", nullable = false)
    private Long workspaceId; // Workspace로 매핑하게 되면 추가적인 SELECT 쿼리 발생가능성 때문에 workspaceId만 저장하도록 설정

    @Column(name = "last_opened")
    private LocalDateTime lastOpened;

    public void updateLastOpened(LocalDateTime lastOpened) {
        this.lastOpened = lastOpened;
    }

}
