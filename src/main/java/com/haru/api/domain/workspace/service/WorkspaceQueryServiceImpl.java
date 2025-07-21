package com.haru.api.domain.workspace.service;

import com.haru.api.domain.meeting.repository.MeetingRepository;
import com.haru.api.domain.moodTracker.repository.MoodTrackerRepository;
import com.haru.api.domain.snsEvent.repository.SnsEventRepository;
import com.haru.api.domain.user.repository.UserRepository;
import com.haru.api.domain.userWorkspace.repository.UserWorkspaceRepository;
import com.haru.api.domain.workspace.dto.WorkspaceResponseDTO;
import com.haru.api.domain.workspace.repository.WorkspaceRepository;
import com.haru.api.global.apiPayload.code.status.ErrorStatus;
import com.haru.api.global.apiPayload.exception.handler.MemberHandler;
import com.haru.api.global.apiPayload.exception.handler.WorkspaceHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class WorkspaceQueryServiceImpl implements WorkspaceQueryService {

    private final MeetingRepository meetingRepository;
    private final SnsEventRepository snsEventRepository;
    private final MoodTrackerRepository moodTrackerRepository;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserWorkspaceRepository userWorkspaceRepository;

    @Transactional(readOnly = true)
    @Override
    public List<WorkspaceResponseDTO.Document> getDocuments(Long userId, Long workspaceId, String title) {

        // 유저 존재 확인
        userRepository.findById(userId)
                .orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));

        // workspace 존재 확인
        workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceHandler(ErrorStatus.WORKSPACE_NOT_FOUND));

        // 유저가 워크스페이스에 속해있는지 확인
        if (!userWorkspaceRepository.existsByWorkspaceIdAndUserId(workspaceId, userId))
            throw new WorkspaceHandler(ErrorStatus.NOT_BELONG_TO_WORKSPACE);


        // 워크스페이스에 속해있는 document 조회 후 List로 반환
        return Stream.of(
                meetingRepository.findDocumentsByTitleLike(title, workspaceId),
                snsEventRepository.findDocumentsByTitleLike(title, workspaceId),
                moodTrackerRepository.findDocumentsByTitleLike(title, workspaceId)
        ).flatMap(List::stream)
        .toList();
    }
}
