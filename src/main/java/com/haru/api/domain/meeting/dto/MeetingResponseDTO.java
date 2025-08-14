package com.haru.api.domain.meeting.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.haru.api.infra.api.entity.SpeechSegment;
import lombok.*;
import com.haru.api.infra.api.entity.AIQuestion;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


public class MeetingResponseDTO {
    @Getter
    @Builder
    public static class createMeetingResponse{
        @JsonSerialize(using = ToStringSerializer.class)
        private Long meetingId;
        private String title;
    }

    @Getter
    @Builder
    public static class getMeetingResponse{
        @JsonSerialize(using = ToStringSerializer.class)
        private Long meetingId;
        private String title;
        private boolean isCreator;
        private LocalDateTime updatedAt;
    }

    @Getter
    @Builder
    public static class getMeetingProceeding{
        @JsonSerialize(using = ToStringSerializer.class)
        private Long userId;
        private String email;
        private String userName;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long workspaceId;
        private String title;
        private String proceeding;
        private LocalDateTime updatedAt;
    }

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class TranscriptResponse {
        private LocalDateTime meetingStartTime;
        private List<Transcript> transcripts;
    }

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Transcript {
        private Long segmentId;
        private String speakerId;
        private String text;
        private LocalDateTime startTime;
        private List<AIQuestionDTO> aiQuestions;

        public static Transcript from(SpeechSegment segment) {
            return Transcript.builder()
                    .segmentId(segment.getId())
                    .speakerId(segment.getSpeakerId())
                    .text(segment.getText())
                    .startTime(segment.getStartTime())
                    .aiQuestions(segment.getAiQuestions().stream()
                            .map(AIQuestionDTO::from)
                            .collect(Collectors.toList()))
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class AIQuestionDTO {
        private Long questionId;
        private String question;

        public static AIQuestionDTO from(AIQuestion aiQuestion) {
            return AIQuestionDTO.builder()
                    .questionId(aiQuestion.getId())
                    .question(aiQuestion.getQuestion())
                    .build();
        }
    }
}
