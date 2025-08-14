package com.haru.api.infra.api.entity;

import com.haru.api.domain.meeting.entity.Meeting;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "speech_segments")
@Getter
@DynamicUpdate
@DynamicInsert
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SpeechSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String speakerId;

    private String text;

    private LocalDateTime startTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @OneToMany(mappedBy = "speechSegment", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AIQuestion> aiQuestions = new ArrayList<>();

    public void addAIQuestion(AIQuestion aiQuestion) {
        this.aiQuestions.add(aiQuestion);
        aiQuestion.setSpeechSegment(this);
    }

    @Override
    public String toString() {
        return String.format("text_id: %d, text: %s", id, text);
    }
}
