package com.haru.api.domain.meeting.entity;

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
@Table(name = "meetings")
@Getter
@DynamicUpdate
@DynamicInsert
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Meeting extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String title;

    // 안건지 요약본
    @Column(columnDefinition="TEXT")
    private String agendaResult;

    // 회의가 끝난 후에 AI 회의록 정리본
    @Column(columnDefinition="TEXT")
    private String proceeding;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User creator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id")
    private Workspace workspace;

    // AI 회의록 정리본 파일
    @Column(columnDefinition = "TEXT")
    private String proceedingKeyName;

    @Column(columnDefinition = "TEXT")
    private String thumbnailKey;

    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MeetingKeyword> meetingKeywords = new ArrayList<>();



    private Meeting(String title, String agendaResult, User user, Workspace workspace) {
        this.title = title;
        this.agendaResult = agendaResult;
        this.creator = user;
        this.workspace = workspace;
    }

    public static Meeting createInitialMeeting(String title, String agendaResult, User user, Workspace workspaces) {
        return new Meeting(title, agendaResult, user, workspaces);
    }
    public void updateTitle(String title) {
        this.title = title;
    }
    public void updateProceeding(String proceeding) {
        this.proceeding = proceeding;
    }
    public void initProceedingKeyName(String proceedingKeyName) {this.proceedingKeyName = proceedingKeyName;}
    public void initThumbnailKey(String thumbnailKey) {this.thumbnailKey = thumbnailKey;}

    // 연관관계 편의 메서드
    public void addTag(Keyword keyword) {
        MeetingKeyword meetingKeyword = MeetingKeyword.builder()
                .meeting(this)
                .keyword(keyword)
                .build();
        this.meetingKeywords.add(meetingKeyword);
    }
}
