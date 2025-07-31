package com.haru.api.domain.workspace.service;

import com.haru.api.domain.lastOpened.repository.UserDocumentLastOpenedRepository;
import com.haru.api.domain.meeting.repository.MeetingRepository;
import com.haru.api.domain.moodTracker.repository.MoodTrackerRepository;
import com.haru.api.domain.snsEvent.repository.SnsEventRepository;
import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.user.repository.UserRepository;
import com.haru.api.domain.userWorkspace.repository.UserWorkspaceRepository;
import com.haru.api.domain.workspace.converter.WorkspaceConverter;
import com.haru.api.domain.workspace.dto.WorkspaceResponseDTO;
import com.haru.api.domain.workspace.repository.WorkspaceRepository;
import com.haru.api.global.apiPayload.code.status.ErrorStatus;
import com.haru.api.global.apiPayload.exception.handler.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class WorkspaceQueryServiceImpl implements WorkspaceQueryService {

    private final MeetingRepository meetingRepository;
    private final SnsEventRepository snsEventRepository;
    private final MoodTrackerRepository moodTrackerRepository;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserWorkspaceRepository userWorkspaceRepository;
    private final UserDocumentLastOpenedRepository userDocumentLastOpenedRepository;

    @Transactional(readOnly = true)
    @Override
    public WorkspaceResponseDTO.DocumentList getDocuments(Long userId, Long workspaceId, String title) {

        // 유저 존재 확인
        User foundUser = userRepository.findById(userId)
                .orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));

        // workspace 존재 확인
        workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceHandler(ErrorStatus.WORKSPACE_NOT_FOUND));

        // 유저가 워크스페이스에 속해있는지 확인
        if (!userWorkspaceRepository.existsByWorkspaceIdAndUserId(workspaceId, userId))
            throw new WorkspaceHandler(ErrorStatus.NOT_BELONG_TO_WORKSPACE);

        // 각 문서별로 title이 일치하는 문서 검색
        List<WorkspaceResponseDTO.Document> meetingList = meetingRepository.findRecentDocumentsByTitle(foundUser.getId(), title);
        List<WorkspaceResponseDTO.Document> snsEventList = snsEventRepository.findRecentDocumentsByTitle(foundUser.getId(), title);
        List<WorkspaceResponseDTO.Document> moodTrackerList = moodTrackerRepository.findRecentDocumentsByTitle(foundUser.getId(), title);

        // 모든 문서 리스트 스트림으로 합침
        List<WorkspaceResponseDTO.Document> allResults = new ArrayList<>();
        allResults.addAll(meetingList);
        allResults.addAll(snsEventList);
        allResults.addAll(moodTrackerList);

        // lastOpened 기준으로 정렬하고 상위 9개 추출
        // last_opened가 null인 경우에는 가장 뒤로 보내기
        // last_opened가 모두 null인 경우에도 동작하도록 구현
        List<WorkspaceResponseDTO.Document> finalResult = allResults.stream()
                .sorted(Comparator.comparing(WorkspaceResponseDTO.Document::getLastOpened,
                                            Comparator.nullsLast(Comparator.naturalOrder()))
                                    .reversed())
                .toList();

        return WorkspaceConverter.toDocumentsDTO(finalResult);

    }
}
