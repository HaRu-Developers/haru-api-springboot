package com.haru.api.domain.moodTracker.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "multiple_choices")
@Getter
@DynamicUpdate
@DynamicInsert
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MultipleChoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_question_id", nullable = false)
    private SurveyQuestion surveyQuestion;

    private String content;

    @OneToMany(mappedBy = "multipleChoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MultipleChoiceAnswer> multipleChoiceAnswers = new ArrayList<>();
}
