package com.haru.api.domain.workspace.entity;

import com.haru.api.domain.meeting.entity.Meeting;
import com.haru.api.domain.moodTracker.entity.MoodTracker;
import com.haru.api.domain.snsEvent.entity.SnsEvent;
import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.userWorkspace.entity.UserWorkspace;
import com.haru.api.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "workspaces")
@Getter
@DynamicUpdate
@DynamicInsert
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Workspace extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Meeting> meetingList = new ArrayList<>();

    @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SnsEvent> snsEventList = new ArrayList<>();

    @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MoodTracker> moodTrackerList = new ArrayList<>();

    public void updateTitle(String title) {
        this.title = title;
    }
}
