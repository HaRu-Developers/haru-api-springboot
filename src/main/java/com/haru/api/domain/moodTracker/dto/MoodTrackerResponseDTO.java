package com.haru.api.domain.moodTracker.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.haru.api.domain.moodTracker.entity.enums.QuestionType;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

public class MoodTrackerResponseDTO {

    @Getter
    @Builder
    public static class PreviewList {
        private List<Preview> moodTrackerList;
    }

    @Getter
    @Builder
    public static class Preview {
        private String moodTrackerHashedId;
        private String title;
        private LocalDateTime updatedAt;
        private LocalDateTime dueDate;
        private Integer respondentsNum;
    }

    @Getter
    @Builder
    public static class CreateResult {
        private String moodTrackerHashedId;
    }

    @Getter
    @SuperBuilder
    public static class BaseResult{
        private String moodTrackerHashedId;
        private String title;
        private String creator;
        private LocalDateTime updatedAt;
        private LocalDateTime dueDate;
        private Integer respondentsNum;
    }

    @Getter
    @SuperBuilder
    public static class QuestionResult extends BaseResult {
        private String description;
        private List<QuestionView> questionList;
        private Long workspaceId;
    }

    @Getter
    @Builder
    public static class QuestionView {
        private Long questionId;
        private String questionTitle;
        private QuestionType type;
        private Boolean isMandatory;

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private List<MultipleChoice> multipleChoiceList;

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private List<CheckboxChoice> checkboxChoiceList;
    }

    @Getter
    @Builder
    public static class MultipleChoice {
        private Long multipleChoiceId;
        private String content;
    }

    @Getter
    @Builder
    public static class CheckboxChoice {
        private Long checkboxChoiceId;
        private String content;
    }

    @Getter
    @SuperBuilder
    public static class ReportResult extends BaseResult {
        private List<String> suggestionList;
        private String report;
        private Long workspaceId;
    }

    @Getter
    @SuperBuilder
    public static class ResponseResult extends BaseResult {
        private List<QuestionResponseView> responseList;
        private Long workspaceId;
    }

    @Getter
    @Builder
    public static class QuestionResponseView {
        private Long questionId;
        private String questionTitle;
        private QuestionType type;

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private List<MultipleChoiceResponse> multipleChoiceResponseList;

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private List<CheckboxChoiceResponse> checkboxChoiceResponseList;

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private List<String> subjectiveResponseList;
    }

    @Getter
    @Builder
    public static class MultipleChoiceResponse {
        private Long multipleChoiceId;
        private String content;
        private Integer selectedNum;
    }

    @Getter
    @Builder
    public static class CheckboxChoiceResponse {
        private Long checkboxChoiceId;
        private String content;
        private Integer selectedNum;
    }
}
