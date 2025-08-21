package com.haru.api.domain.snsEvent.entity;

import com.haru.api.domain.lastOpened.entity.Documentable;
import com.haru.api.domain.lastOpened.entity.enums.DocumentType;
import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.workspace.entity.Workspace;
import com.haru.api.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sns_events")
@Getter
@DynamicUpdate
@DynamicInsert
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SnsEvent extends BaseEntity implements Documentable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    private String snsLink;

    @Column(length = 200)
    private String snsLinkTitle;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User creator;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "workspace_id")
    private Workspace workspace;

    @Column(columnDefinition = "TEXT")
    private String keyNameParticipantPdf;

    @Column(columnDefinition = "TEXT")
    private String keyNameParticipantWord;

    @Column(columnDefinition = "TEXT")
    private String keyNameWinnerPdf;

    @Column(columnDefinition = "TEXT")
    private String keyNameWinnerWord;

    @Column(columnDefinition = "TEXT")
    private String thumbnailKeyName;

    @OneToMany(mappedBy = "snsEvent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Participant> participantList = new ArrayList<>();

    @OneToMany(mappedBy = "snsEvent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Winner> winnerList = new ArrayList<>();

    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
        if (this.workspace != null) {
            workspace.getSnsEventList().remove(this);
        }
        this.workspace.getSnsEventList().add(this);
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateKeyNameParticipantPdf(
            String keyNameParticipantPdf,
            String keyNameParicipantWord,
            String keyNameWinnerPdf,
            String keyNameWinnerWord) {
        this.keyNameParticipantPdf = keyNameParticipantPdf;
        this.keyNameParticipantWord = keyNameParicipantWord;
        this.keyNameWinnerPdf = keyNameWinnerPdf;
        this.keyNameWinnerWord = keyNameWinnerWord;
    }

    public void initThumbnailKeyName(String thumbnailKey) {
        this.thumbnailKeyName = thumbnailKey;
    }

    @Override
    public Long getWorkspaceId() {
        return this.workspace.getId();
    }

    @Override
    public DocumentType getDocumentType() {
        return DocumentType.SNS_EVENT_ASSISTANT;
    }
}
