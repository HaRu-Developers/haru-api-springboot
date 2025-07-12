package com.haru.api.domain.meeting.entity;

import com.haru.api.domain.user.entity.Users;
import com.haru.api.domain.workspace.entity.Workspaces;
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
@AllArgsConstructor
@Builder
public class Meetings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String title;

    //file을 직접 저장하면 db의 용량이 커지고 조회때마다 io가 커지므로 저장하지 않도록 함
    //private String agendaFile;

//    private String agendaResult;

    @Column(columnDefinition="TEXT")
    private String proceeding;

    @ManyToOne(fetch = FetchType.LAZY)
    private Users users;

    @ManyToOne(fetch = FetchType.LAZY)
    private Workspaces workspaces;
}
