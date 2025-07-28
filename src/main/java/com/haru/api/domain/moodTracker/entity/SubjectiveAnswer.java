package com.haru.api.domain.moodTracker.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "subjective_answers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SubjectiveAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_question_id", nullable = false)
    private SurveyQuestion surveyQuestion;

    @Column(columnDefinition = "TEXT")
    private String answer;
}
