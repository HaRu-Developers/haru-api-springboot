package com.haru.api.domain.moodTracker.service;

import com.haru.api.domain.moodTracker.converter.MoodTrackerConverter;
import com.haru.api.domain.moodTracker.dto.MoodTrackerRequestDTO;
import com.haru.api.domain.moodTracker.dto.MoodTrackerResponseDTO;
import com.haru.api.domain.moodTracker.entity.CheckboxChoice;
import com.haru.api.domain.moodTracker.entity.MoodTracker;
import com.haru.api.domain.moodTracker.entity.MultipleChoice;
import com.haru.api.domain.moodTracker.entity.SurveyQuestion;
import com.haru.api.domain.moodTracker.entity.enums.QuestionType;
import com.haru.api.domain.moodTracker.repository.CheckboxChoiceRepository;
import com.haru.api.domain.moodTracker.repository.MoodTrackerRepository;
import com.haru.api.domain.moodTracker.repository.MultipleChoiceRepository;
import com.haru.api.domain.moodTracker.repository.SurveyQuestionRepository;
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

import java.time.LocalDateTime;
import java.util.List;

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

            if (questionDTO.getType() == QuestionType.MULTIPLE_CHOICE) {
                List<MultipleChoice> choices = MoodTrackerConverter.toMultipleChoices(questionDTO.getOptions(), question);
                multipleChoiceRepository.saveAll(choices);
            } else if (questionDTO.getType() == QuestionType.CHECKBOX_CHOICE) {
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

}
