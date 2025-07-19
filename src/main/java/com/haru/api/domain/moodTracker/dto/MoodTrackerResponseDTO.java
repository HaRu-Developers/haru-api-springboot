package com.haru.api.domain.moodTracker.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class MoodTrackerResponseDTO {

    @Getter
    @Builder
    public static class Preview {
        private Long moodTrackerId;
        private String title;
        private LocalDateTime createdAt;
        private LocalDateTime dueDate;
        private Integer respondentsNum;
    }

    @Getter
    @Builder
    public static class CreateResult {
        private Long moodTrackerId;
    }
}
