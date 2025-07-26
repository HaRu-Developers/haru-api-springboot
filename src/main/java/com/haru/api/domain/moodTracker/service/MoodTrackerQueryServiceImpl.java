package com.haru.api.domain.moodTracker.service;

import com.haru.api.domain.moodTracker.converter.MoodTrackerConverter;
import com.haru.api.domain.moodTracker.dto.MoodTrackerResponseDTO;
import com.haru.api.domain.moodTracker.entity.MoodTracker;
import com.haru.api.domain.moodTracker.entity.enums.MoodTrackerVisibility;
import com.haru.api.domain.moodTracker.repository.MoodTrackerRepository;
import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.user.repository.UserRepository;
import com.haru.api.domain.userWorkspace.entity.UserWorkspace;
import com.haru.api.domain.userWorkspace.entity.enums.Auth;
import com.haru.api.domain.userWorkspace.repository.UserWorkspaceRepository;
import com.haru.api.domain.workspace.entity.Workspace;
import com.haru.api.domain.workspace.repository.WorkspaceRepository;
import com.haru.api.global.apiPayload.code.status.ErrorStatus;
import com.haru.api.global.apiPayload.exception.handler.MemberHandler;
import com.haru.api.global.apiPayload.exception.handler.MoodTrackerHandler;
import com.haru.api.global.apiPayload.exception.handler.UserWorkspaceHandler;
import com.haru.api.global.apiPayload.exception.handler.WorkspaceHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MoodTrackerQueryServiceImpl implements MoodTrackerQueryService {
    private final MoodTrackerRepository moodTrackerRepository;

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserWorkspaceRepository userWorkspaceRepository;

    @Override
    public MoodTrackerResponseDTO.PreviewList getMoodTrackerPreviewList(Long userId, Long workspaceId) {
        User foundUser = userRepository.findById(userId)
                .orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));

        Workspace foundWorkspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceHandler(ErrorStatus.WORKSPACE_NOT_FOUND));

        UserWorkspace foundUserWorkspace = userWorkspaceRepository.findByWorkspaceIdAndUserId(foundWorkspace.getId(), foundUser.getId())
                .orElseThrow(() -> new UserWorkspaceHandler(ErrorStatus.USER_WORKSPACE_NOT_FOUND));

        // 모든 분위기 트래커 조회
        List<MoodTracker> foundMoodTrackers = moodTrackerRepository.findAllByWorkspaceId(workspaceId);

        // 권한에 따른 필터링
        List<MoodTracker> accessibleMoodTrackers = foundMoodTrackers.stream()
                .filter(moodTracker ->
                        // 워크스페이스 생성자인 경우 모두 허용
                        foundUserWorkspace.getAuth().equals(Auth.ADMIN)

                        // 또는 해당 설문 생성자인 경우 허용
                        || moodTracker.getCreator().getId().equals(userId)

                        // 또는 공개된 설문인 경우 허용
                        || moodTracker.getVisibility().equals(MoodTrackerVisibility.PUBLIC)
                )
                .collect(Collectors.toList());

        MoodTrackerResponseDTO.PreviewList previewList = MoodTrackerConverter.toPreviewListDTO(foundMoodTrackers);
        return previewList;
    }
}
