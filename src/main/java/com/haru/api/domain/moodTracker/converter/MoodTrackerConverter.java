package com.haru.api.domain.moodTracker.converter;

import com.haru.api.domain.moodTracker.dto.MoodTrackerRequestDTO;
import com.haru.api.domain.moodTracker.dto.MoodTrackerResponseDTO;
import com.haru.api.domain.moodTracker.entity.*;
import com.haru.api.domain.moodTracker.entity.enums.QuestionType;
import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.workspace.entity.Workspace;
import com.haru.api.global.util.HashIdUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class MoodTrackerConverter {

    /**
     * MoodTracker 리스트 → PreviewList DTO 변환
     */
    public static MoodTrackerResponseDTO.PreviewList toPreviewListDTO(List<MoodTracker> moodTrackers, HashIdUtil hashIdUtil) {
        List<MoodTrackerResponseDTO.Preview> previewList = moodTrackers.stream()
                .map(m -> toPreviewDTO(m, hashIdUtil))
                .collect(Collectors.toList());

        return com.haru.api.domain.moodTracker.dto.MoodTrackerResponseDTO.PreviewList.builder()
                .moodTrackerList(previewList)
                .build();
    }

    /**
     * 단일 MoodTracker → Preview DTO 변환
     */
    private static MoodTrackerResponseDTO.Preview toPreviewDTO(MoodTracker moodTracker, HashIdUtil hashIdUtil) {
        return com.haru.api.domain.moodTracker.dto.MoodTrackerResponseDTO.Preview.builder()
                .moodTrackerHashedId(hashIdUtil.encode(moodTracker.getId()))
                .title(moodTracker.getTitle())
                .updatedAt(moodTracker.getCreatedAt())
                .dueDate(moodTracker.getDueDate())
                .respondentsNum(moodTracker.getRespondentsNum())
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
    public static List<MultipleChoice> toMultipleChoiceList(
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
    public static List<CheckboxChoice> toCheckboxChoiceList(
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
    public static MoodTrackerResponseDTO.CreateResult toCreateResultDTO(MoodTracker moodTracker, HashIdUtil hashIdUtil) {
        return com.haru.api.domain.moodTracker.dto.MoodTrackerResponseDTO.CreateResult.builder()
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
    public static List<CheckboxChoiceAnswer> toCheckboxChoiceAnswerList(
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

    /**
     *  분위기 트래커 리포트 DTO 변환
     */
    public static MoodTrackerResponseDTO.ReportResult toReportResultDTO(MoodTracker moodTracker, List<String> suggestionList, HashIdUtil hashIdUtil) {
        return MoodTrackerResponseDTO.ReportResult.builder()
                .workspaceId(moodTracker.getWorkspace().getId())
                .moodTrackerHashedId(hashIdUtil.encode(moodTracker.getId()))
                .title(moodTracker.getTitle())
                .creatorId(moodTracker.getCreator().getId())
                .creatorName(moodTracker.getCreator().getName())
                .updatedAt(moodTracker.getUpdatedAt())
                .dueDate(moodTracker.getDueDate())
                .respondentsNum(moodTracker.getRespondentsNum())
                .report(moodTracker.getReport())
                .suggestionList(suggestionList)
                .build();
    }

    /**
     * 분위기 트래커 질문 DTO 반환
     */
    public static MoodTrackerResponseDTO.QuestionResult toQuestionResultDTO(MoodTracker moodTracker, List<SurveyQuestion> questionList, HashIdUtil hashIdUtil) {
        List<MoodTrackerResponseDTO.QuestionView> questionViewList = questionList.stream()
                .map(question -> {
                    List<MoodTrackerResponseDTO.MultipleChoice> multipleChoices = question.getMultipleChoiceList().stream()
                            .map(choice -> MoodTrackerResponseDTO.MultipleChoice.builder()
                                    .multipleChoiceId(choice.getId())
                                    .content(choice.getContent())
                                    .build())
                            .collect(Collectors.toList());

                    List<MoodTrackerResponseDTO.CheckboxChoice> checkboxChoices = question.getCheckboxChoiceList().stream()
                            .map(choice -> MoodTrackerResponseDTO.CheckboxChoice.builder()
                                    .checkboxChoiceId(choice.getId())
                                    .content(choice.getContent())
                                    .build())
                            .collect(Collectors.toList());

                    return MoodTrackerResponseDTO.QuestionView.builder()
                            .questionId(question.getId())
                            .questionTitle(question.getTitle())
                            .type(question.getType())
                            .isMandatory(question.getIsMandatory())
                            .multipleChoiceList(multipleChoices)
                            .checkboxChoiceList(checkboxChoices)
                            .build();
                })
                .collect(Collectors.toList());

        return MoodTrackerResponseDTO.QuestionResult.builder()
                .workspaceId(moodTracker.getWorkspace().getId())
                .moodTrackerHashedId(hashIdUtil.encode(moodTracker.getId()))
                .title(moodTracker.getTitle())
                .creatorId(moodTracker.getCreator().getId())
                .creatorName(moodTracker.getCreator().getName())
                .updatedAt(moodTracker.getUpdatedAt())
                .dueDate(moodTracker.getDueDate())
                .respondentsNum(moodTracker.getRespondentsNum())
                .description(moodTracker.getDescription())
                .questionList(questionViewList)
                .build();
    }

    /**
     * 분위기 트래커 응답 결과 DTO 변환
     */
    public static MoodTrackerResponseDTO.ResponseResult toResponseResultDTO(MoodTracker moodTracker, List<SurveyQuestion> questions, HashIdUtil hashIdUtil) {
        List<MoodTrackerResponseDTO.QuestionResponseView> responseViews = questions.stream()
                .map(q -> {
                    if (q.getType() == QuestionType.MULTIPLE_CHOICE) {
                        List<MoodTrackerResponseDTO.MultipleChoiceResponse> responses = q.getMultipleChoiceList().stream()
                                .map(choice -> MoodTrackerResponseDTO.MultipleChoiceResponse.builder()
                                        .multipleChoiceId(choice.getId())
                                        .content(choice.getContent())
                                        .selectedNum(choice.getMultipleChoiceAnswerList().size())
                                        .build())
                                .collect(Collectors.toList());

                        return MoodTrackerResponseDTO.QuestionResponseView.builder()
                                .questionId(q.getId())
                                .questionTitle(q.getTitle())
                                .type(q.getType())
                                .multipleChoiceResponseList(responses)
                                .build();

                    } else if (q.getType() == QuestionType.CHECKBOX_CHOICE) {
                        List<MoodTrackerResponseDTO.CheckboxChoiceResponse> responses = q.getCheckboxChoiceList().stream()
                                .map(choice -> MoodTrackerResponseDTO.CheckboxChoiceResponse.builder()
                                        .checkboxChoiceId(choice.getId())
                                        .content(choice.getContent())
                                        .selectedNum(choice.getCheckboxChoiceAnswerList().size())
                                        .build())
                                .collect(Collectors.toList());

                        return MoodTrackerResponseDTO.QuestionResponseView.builder()
                                .questionId(q.getId())
                                .questionTitle(q.getTitle())
                                .type(q.getType())
                                .checkboxChoiceResponseList(responses)
                                .build();

                    } else { // SUBJECTIVE
                        List<String> subjectiveResponses = q.getSubjectiveAnswerList().stream()
                                .map(SubjectiveAnswer::getAnswer)
                                .collect(Collectors.toList());

                        return MoodTrackerResponseDTO.QuestionResponseView.builder()
                                .questionId(q.getId())
                                .questionTitle(q.getTitle())
                                .type(q.getType())
                                .subjectiveResponseList(subjectiveResponses)
                                .build();
                    }
                })
                .collect(Collectors.toList());

        return MoodTrackerResponseDTO.ResponseResult.builder()
                .workspaceId(moodTracker.getWorkspace().getId())
                .moodTrackerHashedId(hashIdUtil.encode(moodTracker.getId()))
                .title(moodTracker.getTitle())
                .creatorId(moodTracker.getCreator().getId())
                .creatorName(moodTracker.getCreator().getName())
                .updatedAt(moodTracker.getUpdatedAt())
                .dueDate(moodTracker.getDueDate())
                .respondentsNum(moodTracker.getRespondentsNum())
                .responseList(responseViews)
                .build();
    }


}
