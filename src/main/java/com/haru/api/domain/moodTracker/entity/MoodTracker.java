package com.haru.api.domain.moodTracker.entity;

import com.haru.api.domain.moodTracker.entity.enums.MoodTrackerVisibility;
import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.workspace.entity.Workspace;
import com.haru.api.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

@Entity
@Table(name = "mood_trackers")
@Getter
@DynamicUpdate
@DynamicInsert
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MoodTracker extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 설문ID

    // 🔹 만든 사람 (User)와 N:1 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 🔹 작업공간과 N:1 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(length = 50)
    private String title; // 설문명

    @Column(columnDefinition = "TEXT")
    private String description; // 설문소개

    @Column(name = "due_date")
    private LocalDateTime dueDate; // 마감일

    @Column(length = 10)
    private MoodTrackerVisibility visibility; // 공개범위 (PUBLIC, PRIVATE)

    @Column(name = "survey_link", length = 100)
    private String surveyLink; // 설문 링크

    @Column(columnDefinition = "TEXT")
    private String report; // 리포트

    @Column(columnDefinition = "TEXT")
    private String suggestion; // 하루제안
}
