package com.haru.api.domain.moodTracker.converter;

import com.haru.api.domain.moodTracker.dto.MoodTrackerRequestDTO;
import com.haru.api.domain.moodTracker.dto.MoodTrackerResponseDTO;
import com.haru.api.domain.moodTracker.entity.CheckboxChoice;
import com.haru.api.domain.moodTracker.entity.MoodTracker;
import com.haru.api.domain.moodTracker.entity.MultipleChoice;
import com.haru.api.domain.moodTracker.entity.SurveyQuestion;
import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.workspace.entity.Workspace;

import java.util.ArrayList;
import java.util.List;

public class MoodTrackerConverter {
    /**
     * MoodTracker 생성용 변환
     */
    public static MoodTracker toMoodTracker(
            MoodTrackerRequestDTO.CreateRequest dto,
            User user,
            Workspace workspace
    ) {
        return MoodTracker.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .dueDate(dto.getDueDate())
                .visibility(dto.getVisibility())
                .workspace(workspace)
                .creator(user)
                .respondentsNum(0)
                .build();
    }

    /**
     * SurveyQuestion 변환
     */
    public static SurveyQuestion toSurveyQuestion(
            MoodTrackerRequestDTO.SurveyQuestion dto,
            MoodTracker moodTracker
    ) {
        return SurveyQuestion.builder()
                .moodTracker(moodTracker)
                .title(dto.getTitle())
                .type(dto.getType())
                .isMandatory(dto.getIsMandatory())
                .build();
    }

    /**
     * 객관식 보기 변환
     */
    public static List<MultipleChoice> toMultipleChoices(
            List<String> options,
            SurveyQuestion question
    ) {
        List<MultipleChoice> choices = new ArrayList<>();
        for (String content : options) {
            choices.add(MultipleChoice.builder()
                    .surveyQuestion(question)
                    .content(content)
                    .build());
        }
        return choices;
    }

    /**
     * 체크박스 보기 변환
     */
    public static List<CheckboxChoice> toCheckboxChoices(
            List<String> options,
            SurveyQuestion question
    ) {
        List<CheckboxChoice> choices = new ArrayList<>();
        for (String content : options) {
            choices.add(CheckboxChoice.builder()
                    .surveyQuestion(question)
                    .content(content)
                    .build());
        }
        return choices;
    }

    /**
     * 생성 결과 반환
     */
    public static MoodTrackerResponseDTO.CreateResult toCreateResult(MoodTracker moodTracker) {
        return MoodTrackerResponseDTO.CreateResult.builder()
                .moodTrackerId(moodTracker.getId())
                .build();
    }
}
