package com.haru.api.domain.meeting.entity;

import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.workspace.entity.Workspace;
import com.haru.api.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

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

    //file을 직접 저장하면 db의 용량이 커지고 조회때마다 io가 커지므로 저장하지 않도록 함
    //private String agendaFile;

    // 안건지 요약본
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
}
