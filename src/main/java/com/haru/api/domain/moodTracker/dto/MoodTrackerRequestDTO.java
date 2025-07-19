package com.haru.api.domain.moodTracker.dto;

import com.haru.api.domain.moodTracker.entity.enums.MoodTrackerVisibility;
import com.haru.api.domain.moodTracker.entity.enums.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

@Validated
public class MoodTrackerRequestDTO {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank
        private String title;

        private String description;

        @NotNull
        private LocalDateTime dueDate;

        @NotNull
        private MoodTrackerVisibility visibility; // ENUM(PUBLIC, PRIVATE)

        @NotEmpty
        private List<SurveyQuestion> questions; // 하위 문항 리스트
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SurveyQuestion {

        @NotBlank
        private String title;

        @NotNull
        private QuestionType type; // SHORT_ANSWER, MULTIPLE_CHOICE, CHECKBOX

        private Boolean isMandatory;

        // MULTIPLE_CHOICE 또는 CHECKBOX일 경우에만 사용
        private List<String> options;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateTitleRequest {
        @NotBlank
        private String title;
    }
}
