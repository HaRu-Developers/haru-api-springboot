package com.haru.api.domain.moodTracker.service;

import com.haru.api.domain.moodTracker.converter.MoodTrackerConverter;
import com.haru.api.domain.moodTracker.dto.MoodTrackerRequestDTO;
import com.haru.api.domain.moodTracker.dto.MoodTrackerResponseDTO;
import com.haru.api.domain.moodTracker.entity.*;
import com.haru.api.domain.moodTracker.repository.*;
import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.user.repository.UserRepository;
import com.haru.api.domain.workspace.entity.Workspace;
import com.haru.api.domain.workspace.repository.WorkspaceRepository;
import com.haru.api.global.apiPayload.code.status.ErrorStatus;
import com.haru.api.global.apiPayload.exception.handler.MemberHandler;
import com.haru.api.global.apiPayload.exception.handler.MoodTrackerHandler;
import com.haru.api.global.apiPayload.exception.handler.WorkspaceHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.haru.api.domain.moodTracker.entity.enums.QuestionType.*;

@Service
@RequiredArgsConstructor
@Transactional
public class MoodTrackerCommandServiceImpl implements MoodTrackerCommandService {

    private final MoodTrackerRepository moodTrackerRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;

    private final SurveyQuestionRepository surveyQuestionRepository;
    private final MultipleChoiceRepository multipleChoiceRepository;
    private final CheckboxChoiceRepository checkboxChoiceRepository;

    private final MoodTrackerMailService moodTrackerMailService;

    private final MultipleChoiceAnswerRepository multipleChoiceAnswerRepository;
    private final CheckboxChoiceAnswerRepository checkboxChoiceAnswerRepository;
    private final SubjectiveAnswerRepository subjectiveAnswerRepository;

    /**
     * 분위기 트래커 생성
     */
    @Override
    public MoodTrackerResponseDTO.CreateResult create(
            Long userId,
            Long workspaceId,
            MoodTrackerRequestDTO.CreateRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceHandler(ErrorStatus.WORKSPACE_NOT_FOUND));

        if (userId != workspace.getCreator().getId())
            throw new MoodTrackerHandler(ErrorStatus.MOOD_TRACKER_MODIFY_NOT_ALLOWED);

        // 분위기 트래커 생성 및 저장
        MoodTracker moodTracker = MoodTrackerConverter.toMoodTracker(request, user, workspace);
        moodTrackerRepository.save(moodTracker);

        // 선택지 생성 및 저장
        for (MoodTrackerRequestDTO.SurveyQuestion questionDTO : request.getQuestions()) {
            SurveyQuestion question = MoodTrackerConverter.toSurveyQuestion(questionDTO, moodTracker);
            surveyQuestionRepository.save(question);

            if (questionDTO.getType() == MULTIPLE_CHOICE) {
                List<MultipleChoice> choices = MoodTrackerConverter.toMultipleChoices(questionDTO.getOptions(), question);
                multipleChoiceRepository.saveAll(choices);
            } else if (questionDTO.getType() == CHECKBOX_CHOICE) {
                List<CheckboxChoice> choices = MoodTrackerConverter.toCheckboxChoices(questionDTO.getOptions(), question);
                checkboxChoiceRepository.saveAll(choices);
            }
        }

        return MoodTrackerConverter.toCreateResult(moodTracker);
    }

    /**
     * 분위기 트래커 제목 수정
     */
    @Override
    public void updateTitle(Long userId,
                            Long moodTrackerId,
                            MoodTrackerRequestDTO.UpdateTitleRequest request
    ) {
        MoodTracker moodTracker = moodTrackerRepository.findById(moodTrackerId)
                .orElseThrow(() -> new MoodTrackerHandler(ErrorStatus.MOOD_TRACKER_NOT_FOUND));

        if (userId != moodTracker.getCreator().getId())
            throw new MoodTrackerHandler(ErrorStatus.MOOD_TRACKER_MODIFY_NOT_ALLOWED);

        moodTracker.updateTitle(request.getTitle());
    }

    /**
     * 분위기 트래커 삭제
     */
    @Override
    public void delete(
            Long userId,
            Long moodTrackerId
    ) {
        MoodTracker moodTracker = moodTrackerRepository.findById(moodTrackerId)
                .orElseThrow(() -> new MoodTrackerHandler(ErrorStatus.MOOD_TRACKER_NOT_FOUND));

        if (userId != moodTracker.getCreator().getId())
            throw new MoodTrackerHandler(ErrorStatus.MOOD_TRACKER_MODIFY_NOT_ALLOWED);

        moodTrackerRepository.delete(moodTracker);
    }

    /**
     * 분위기 트래커 설문 링크 메일 전송
     */
    @Override
    public void sendSurveyLink(
            Long moodTrackerId
    ) {
        MoodTracker moodTracker = moodTrackerRepository.findById(moodTrackerId)
            .orElseThrow(() -> new MoodTrackerHandler(ErrorStatus.MOOD_TRACKER_NOT_FOUND));

        String creatorName = moodTracker.getCreator().getName();  // 작성자 이름
        String surveyTitle = moodTracker.getTitle();              // 설문 제목

        String mailTitle = "%s 님이 나에게 [%s] 설문을 공유했습니다.".formatted(creatorName, surveyTitle);
        String mailContent = "%s 님의 [%s] 설문에 대한 소중한 의견을 보내주세요.".formatted(creatorName, surveyTitle);

        moodTrackerMailService.sendSurveyLinkToEmail(moodTrackerId, mailTitle, mailContent);
    }

    /**
     * 분위기 트래커 답변 제출
     */
    @Override
    public void submitSurveyAnswers(
            Long moodTrackerId,
            MoodTrackerRequestDTO.SurveyAnswerList request
    ) {
        List<SubjectiveAnswer> subjectiveAnswers = new ArrayList<>();
        List<MultipleChoiceAnswer> multipleChoiceAnswers = new ArrayList<>();
        List<CheckboxChoiceAnswer> checkboxChoiceAnswers = new ArrayList<>();

        // 전체 질문을 미리 조회 및 맵에 캐싱
        List<SurveyQuestion> allQuestions = surveyQuestionRepository.findAllByMoodTrackerId(moodTrackerId);
        Map<Long, SurveyQuestion> questionMap = allQuestions.stream()
                .collect(Collectors.toMap(SurveyQuestion::getId, q -> q));

        // 응답한 질문 ID 수집용
        Set<Long> answeredQuestionIds = new HashSet<>();

        for (MoodTrackerRequestDTO.SurveyAnswer dto : request.getAnswers()) {

            // 질문 엔티티 조회
            SurveyQuestion surveyQuestion = questionMap.get(dto.getQuestionId());
            if (surveyQuestion == null) {
                throw new MoodTrackerHandler(ErrorStatus.SURVEY_QUESTION_NOT_FOUND);
            }

            switch (dto.getType()) {
                case MULTIPLE_CHOICE -> {
                    // 선택지 엔티티 조회 후 추가
                    MultipleChoice multipleChoice = multipleChoiceRepository.findById(dto.getMultipleChoiceId())
                            .orElseThrow();
                    multipleChoiceAnswers.add(
                            MoodTrackerConverter.toMultipleChoiceAnswer(multipleChoice)
                    );
                }
                case CHECKBOX_CHOICE -> {
                    // 체크박스 선택지 엔티티 목록 조회 후 추가
                    List<CheckboxChoice> checkboxChoices = checkboxChoiceRepository.findAllById(dto.getCheckboxChoiceIdList());
                    checkboxChoiceAnswers.addAll(
                            MoodTrackerConverter.toCheckboxChoiceAnswers(checkboxChoices)
                    );
                }
                case SUBJECTIVE -> {
                    // 주관식 답변 추가
                    subjectiveAnswers.add(
                            MoodTrackerConverter.toSubjectiveAnswer(surveyQuestion, dto.getSubjectiveAnswer())
                    );
                }
            }

            // 응답한 questionId 기록
            answeredQuestionIds.add(dto.getQuestionId());
        }

        // 필수 응답 누락 검사
        for (SurveyQuestion question : allQuestions) {
            if (Boolean.TRUE.equals(question.getIsMandatory())
                    && !answeredQuestionIds.contains(question.getId())) {
                throw new MoodTrackerHandler(ErrorStatus.SURVEY_ANSWER_REQUIRED);
            }
        }

        // 일괄 저장
        multipleChoiceAnswerRepository.saveAll(multipleChoiceAnswers);
        checkboxChoiceAnswerRepository.saveAll(checkboxChoiceAnswers);
        subjectiveAnswerRepository.saveAll(subjectiveAnswers);
    }
}
