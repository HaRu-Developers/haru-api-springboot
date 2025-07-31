package com.haru.api.domain.snsEvent.entity;

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
public class SnsEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 255)
    private String snsLink;

    @Column(length = 200)
    private String snsLinkTitle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User creator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id")
    private Workspace workspace;

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
}
