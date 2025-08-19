package com.haru.api.domain.moodTracker.service;

import com.haru.api.domain.moodTracker.converter.MoodTrackerConverter;
import com.haru.api.domain.moodTracker.dto.MoodTrackerResponseDTO;
import com.haru.api.domain.moodTracker.entity.MoodTracker;
import com.haru.api.domain.moodTracker.entity.SurveyQuestion;
import com.haru.api.domain.moodTracker.entity.enums.MoodTrackerVisibility;
import com.haru.api.domain.moodTracker.repository.*;
import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.userWorkspace.entity.UserWorkspace;
import com.haru.api.domain.userWorkspace.entity.enums.Auth;
import com.haru.api.domain.userWorkspace.repository.UserWorkspaceRepository;
import com.haru.api.domain.workspace.entity.Workspace;
import com.haru.api.global.annotation.TrackLastOpened;
import com.haru.api.global.apiPayload.code.status.ErrorStatus;
import com.haru.api.global.apiPayload.exception.handler.MoodTrackerHandler;
import com.haru.api.global.apiPayload.exception.handler.UserWorkspaceHandler;
import com.haru.api.global.util.HashIdUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MoodTrackerQueryServiceImpl implements MoodTrackerQueryService {
    private final MoodTrackerRepository moodTrackerRepository;

    private final UserWorkspaceRepository userWorkspaceRepository;

    private final HashIdUtil hashIdUtil;

    private final SurveyQuestionRepository surveyQuestionRepository;

    @Override
    public MoodTrackerResponseDTO.PreviewList getPreviewList(User user, Workspace workspace) {

        UserWorkspace foundUserWorkspace = userWorkspaceRepository.findByWorkspaceIdAndUserId(workspace.getId(), user.getId())
                .orElseThrow(() -> new UserWorkspaceHandler(ErrorStatus.USER_WORKSPACE_NOT_FOUND));

        // 모든 분위기 트래커 조회
        List<MoodTracker> foundMoodTrackers = moodTrackerRepository.findAllByWorkspaceId(workspace.getId());

        // 권한에 따른 필터링
        List<MoodTracker> accessibleMoodTrackers = foundMoodTrackers.stream()
                .filter(moodTracker ->
                        // 워크스페이스 생성자인 경우 모두 허용
                        foundUserWorkspace.getAuth().equals(Auth.ADMIN)

                        // 또는 해당 설문 생성자인 경우 허용
                        || moodTracker.getCreator().getId().equals(user.getId())

                        // 또는 공개된 설문인 경우 허용
                        || moodTracker.getVisibility().equals(MoodTrackerVisibility.PUBLIC)
                )
                .collect(Collectors.toList());

        MoodTrackerResponseDTO.PreviewList previewList = MoodTrackerConverter.toPreviewListDTO(accessibleMoodTrackers, hashIdUtil);
        return previewList;
    }

    @Override
    @Transactional(readOnly = true)
    public MoodTrackerResponseDTO.BaseResult getBaseResult(Long moodTrackerId) {
        MoodTracker foundMoodTracker = moodTrackerRepository.findById(moodTrackerId)
                .orElseThrow(() -> new MoodTrackerHandler(ErrorStatus.MOOD_TRACKER_NOT_FOUND));

        return MoodTrackerConverter.toBaseResultDTO(foundMoodTracker, hashIdUtil);
    }

    @Override
    @Transactional(readOnly = true)
    public MoodTrackerResponseDTO.QuestionResult getQuestionResult(Long moodTrackerId) {
        MoodTracker foundMoodTracker = moodTrackerRepository.findById(moodTrackerId)
                .orElseThrow(() -> new MoodTrackerHandler(ErrorStatus.MOOD_TRACKER_NOT_FOUND));

        List<SurveyQuestion> questionList = surveyQuestionRepository.findAllByMoodTrackerId(foundMoodTracker.getId());

        return MoodTrackerConverter.toQuestionResultDTO(foundMoodTracker, questionList, hashIdUtil);
    }

    @Override
    @Transactional(readOnly = true)
    @TrackLastOpened
    public MoodTrackerResponseDTO.ReportResult getReportResult(User user, MoodTracker moodTracker) {

        // 권한 확인
        UserWorkspace userWorkspace = userWorkspaceRepository.findByWorkspaceIdAndUserId(
                moodTracker.getWorkspace().getId(), user.getId()
        ).orElseThrow(() -> new UserWorkspaceHandler(ErrorStatus.USER_WORKSPACE_NOT_FOUND));

        boolean hasAccess =
                userWorkspace.getAuth().equals(Auth.ADMIN) ||
                        moodTracker.getCreator().getId().equals(user.getId()) ||
                        moodTracker.getVisibility().equals(MoodTrackerVisibility.PUBLIC);

        if (!hasAccess) {
            throw new MoodTrackerHandler(ErrorStatus.MOOD_TRACKER_ACCESS_DENIED);
        }

        // 마감 여부 확인
        if (LocalDateTime.now().isBefore(moodTracker.getDueDate())) {
            throw new MoodTrackerHandler(ErrorStatus.MOOD_TRACKER_NOT_FINISHED);
        }

        List<String> suggestionList = surveyQuestionRepository.findAllByMoodTrackerId(moodTracker.getId()).stream()
                .map(SurveyQuestion::getSuggestion)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return MoodTrackerConverter.toReportResultDTO(moodTracker, suggestionList, hashIdUtil);
    }

    @Override
    @Transactional(readOnly = true)
    @TrackLastOpened
    public MoodTrackerResponseDTO.ResponseResult getResponseResult(User user, MoodTracker moodTracker) {

        // 권한 확인
        UserWorkspace userWorkspace = userWorkspaceRepository.findByWorkspaceIdAndUserId(
                moodTracker.getWorkspace().getId(), user.getId()
        ).orElseThrow(() -> new UserWorkspaceHandler(ErrorStatus.USER_WORKSPACE_NOT_FOUND));

        boolean hasAccess =
                userWorkspace.getAuth().equals(Auth.ADMIN) ||
                        moodTracker.getCreator().getId().equals(user.getId()) ||
                        moodTracker.getVisibility().equals(MoodTrackerVisibility.PUBLIC);

        if (!hasAccess) {
            throw new MoodTrackerHandler(ErrorStatus.MOOD_TRACKER_ACCESS_DENIED);
        }

        // 마감 여부 확인
        if (LocalDateTime.now().isBefore(moodTracker.getDueDate())) {
            throw new MoodTrackerHandler(ErrorStatus.MOOD_TRACKER_NOT_FINISHED);
        }

        List<SurveyQuestion> questions = surveyQuestionRepository.findAllByMoodTrackerId(moodTracker.getId());

        return MoodTrackerConverter.toResponseResultDTO(moodTracker, questions, hashIdUtil);
    }
}
