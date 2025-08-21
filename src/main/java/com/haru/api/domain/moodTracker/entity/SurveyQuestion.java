package com.haru.api.domain.moodTracker.entity;

import com.haru.api.domain.moodTracker.entity.enums.QuestionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "survey_questions")
@Getter
@DynamicUpdate
@DynamicInsert
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SurveyQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mood_tracker_id", nullable = false)
    private MoodTracker moodTracker; // 분위기 트래커 ID (외래키, Survey 엔티티가 있다면 @ManyToOne 처리 가능)

    @Column(length = 100)
    private String title;

    @Enumerated(EnumType.STRING)
    private QuestionType type;

    @Column(name = "is_mandatory", nullable = false)
    private Boolean isMandatory;

    @Column(name = "suggestion", columnDefinition = "TEXT")
    private String suggestion;

    @OneToMany(mappedBy = "surveyQuestion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MultipleChoice> multipleChoiceList = new ArrayList<>();

    @OneToMany(mappedBy = "surveyQuestion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CheckboxChoice> checkboxChoiceList = new ArrayList<>();

    @OneToMany(mappedBy = "surveyQuestion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SubjectiveAnswer> subjectiveAnswerList = new ArrayList<>();

    public void createSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }
}

