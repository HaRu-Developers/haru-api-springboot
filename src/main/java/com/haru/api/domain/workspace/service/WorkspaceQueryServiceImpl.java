package com.haru.api.domain.workspace.service;

import com.haru.api.domain.lastOpened.entity.UserDocumentLastOpened;
import com.haru.api.domain.lastOpened.entity.enums.DocumentType;
import com.haru.api.domain.lastOpened.repository.UserDocumentLastOpenedRepository;
import com.haru.api.domain.meeting.entity.Meeting;
import com.haru.api.domain.meeting.repository.MeetingRepository;
import com.haru.api.domain.moodTracker.entity.MoodTracker;
import com.haru.api.domain.moodTracker.repository.MoodTrackerRepository;
import com.haru.api.domain.snsEvent.entity.SnsEvent;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

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

        // last opened에서 최대 9개 조회
        List<UserDocumentLastOpened> recentDocumentList = userDocumentLastOpenedRepository.findTop9ByUserIdOrderByLastOpenedDateDesc(foundUser.getId());

        // DocumentType 별로 ID를 그룹화
        Map<DocumentType, List<Long>> documentIdsByType = recentDocumentList.stream()
                .collect(Collectors.groupingBy(
                        UserDocumentLastOpened::getDocumentType,
                        Collectors.mapping(UserDocumentLastOpened::getDocumentId, Collectors.toList())
                ));

        // 각 타입별로 필요한 엔티티를 한 번에 조회하여 Map으로 변환
        Map<Long, Meeting> meetingMap = meetingRepository.findAllById(
                documentIdsByType.getOrDefault(DocumentType.AI_MEETING_MANAGER, Collections.emptyList())
        ).stream().collect(Collectors.toMap(Meeting::getId, Function.identity()));

        Map<Long, SnsEvent> snsEventMap = snsEventRepository.findAllById(
                documentIdsByType.getOrDefault(DocumentType.SNS_EVENT_ASSISTANT, Collections.emptyList())
        ).stream().collect(Collectors.toMap(SnsEvent::getId, Function.identity()));

        Map<Long, MoodTracker> moodTrackerMap = moodTrackerRepository.findAllById(
                documentIdsByType.getOrDefault(DocumentType.TEAM_MOOD_TRACKER, Collections.emptyList())
        ).stream().collect(Collectors.toMap(MoodTracker::getId, Function.identity()));

        // 메모리 상에서 DTO로 변환
        List<WorkspaceResponseDTO.Document> documentList = recentDocumentList.stream()
                .map(recentDocument -> {
                    switch (recentDocument.getDocumentType()) {
                        case AI_MEETING_MANAGER:
                            Meeting foundMeeting = meetingMap.get(recentDocument.getDocumentId());
                            if (foundMeeting != null) {
                                return WorkspaceConverter.toDocument(foundMeeting, recentDocument.getLastOpened());
                            }
                            break;
                        case SNS_EVENT_ASSISTANT:
                            SnsEvent foundSnsEvent = snsEventMap.get(recentDocument.getDocumentId());
                            if (foundSnsEvent != null) {
                                return WorkspaceConverter.toDocument(foundSnsEvent, recentDocument.getLastOpened());
                            }
                            break;
                        case TEAM_MOOD_TRACKER:
                            MoodTracker foundMoodTracker = moodTrackerMap.get(recentDocument.getDocumentId());
                            if (foundMoodTracker != null) {
                                return WorkspaceConverter.toDocument(foundMoodTracker, recentDocument.getLastOpened());
                            }
                            break;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .toList();

        // 최종 DTO 반환
        return WorkspaceConverter.toDocumentsDTO(documentList);
    }
}
