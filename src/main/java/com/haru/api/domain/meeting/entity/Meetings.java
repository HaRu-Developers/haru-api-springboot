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
public class Meetings extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String title;

    //file을 직접 저장하면 db의 용량이 커지고 조회때마다 io가 커지므로 저장하지 않도록 함
    //private String agendaFile;

    private String agendaResult;

    @Column(columnDefinition="TEXT")
    private String proceeding;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    private Workspace workspaces;

    private Meetings(String title, String agendaResult, User user, Workspace workspaces) {
        this.title = title;
        this.agendaResult = agendaResult;
        this.user = user;
        this.workspaces = workspaces;
    }

    public static Meetings createInitialMeeting(String title, String agendaResult, User user, Workspace workspaces) {
        return new Meetings(title, agendaResult, user, workspaces);
    }
    public void updateTitle(String title) {
        this.title = title;
    }
    public void updateProceeding(String proceeding) {
        this.proceeding = proceeding;
    }
}
