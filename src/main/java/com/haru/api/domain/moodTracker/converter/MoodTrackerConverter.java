package com.haru.api.domain.moodTracker.converter;

import com.haru.api.domain.moodTracker.dto.MoodTrackerRequestDTO;
import com.haru.api.domain.moodTracker.dto.MoodTrackerResponseDTO;
import com.haru.api.domain.moodTracker.entity.*;
import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.workspace.entity.Workspace;
import com.haru.api.global.util.HashIdUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class MoodTrackerConverter {

    /**
     * 단일 MoodTracker → Preview DTO 변환
     */
    public static MoodTrackerResponseDTO.Preview toPreviewDTO(MoodTracker moodTracker, HashIdUtil hashIdUtil) {
        return MoodTrackerResponseDTO.Preview.builder()
                .moodTrackerHashedId(hashIdUtil.encode(moodTracker.getId()))
                .title(moodTracker.getTitle())
                .updatedAt(moodTracker.getCreatedAt())
                .dueDate(moodTracker.getDueDate())
                .respondentsNum(moodTracker.getRespondentsNum())
                .build();
    }

    /**
     * MoodTracker 리스트 → PreviewList DTO 변환
     */
    public static MoodTrackerResponseDTO.PreviewList toPreviewListDTO(List<MoodTracker> moodTrackers, HashIdUtil hashIdUtil) {
        List<MoodTrackerResponseDTO.Preview> previewList = moodTrackers.stream()
                .map(m -> toPreviewDTO(m, hashIdUtil))
                .collect(Collectors.toList());

        return MoodTrackerResponseDTO.PreviewList.builder()
                .moodTrackerList(previewList)
                .build();
    }

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
    public static MoodTrackerResponseDTO.CreateResult toCreateResult(MoodTracker moodTracker, HashIdUtil hashIdUtil) {
        return MoodTrackerResponseDTO.CreateResult.builder()
                .moodTrackerHashedId(hashIdUtil.encode(moodTracker.getId()))
                .build();
    }

    /**
     * 주관식 답변 변환
     */
    public static SubjectiveAnswer toSubjectiveAnswer(
            SurveyQuestion question,
            String answerText
    ) {
        return SubjectiveAnswer.builder()
                .surveyQuestion(question)
                .answer(answerText)
                .build();
    }

    /**
     * 객관식 단답 (Multiple Choice) 변환
     */
    public static MultipleChoiceAnswer toMultipleChoiceAnswer(
            MultipleChoice multipleChoice
    ) {
        return MultipleChoiceAnswer.builder()
                    .multipleChoice(multipleChoice)
                    .build();
    }

    /**
     * 객관식 복수답 (Checkbox) 변환
     */
    public static List<CheckboxChoiceAnswer> toCheckboxChoiceAnswers(
            List<CheckboxChoice> checkboxChoices
    ) {
        List<CheckboxChoiceAnswer> answers = new ArrayList<>();
        for (CheckboxChoice checkboxChoice : checkboxChoices) {
            answers.add(CheckboxChoiceAnswer.builder()
                    .checkboxChoice(checkboxChoice)
                    .build());
        }
        return answers;
    }
}
