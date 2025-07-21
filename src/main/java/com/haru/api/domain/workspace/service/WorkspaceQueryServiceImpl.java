package com.haru.api.domain.workspace.service;

import com.haru.api.domain.meeting.repository.MeetingRepository;
import com.haru.api.domain.moodTracker.repository.MoodTrackerRepository;
import com.haru.api.domain.snsEvent.repository.SnsEventRepository;
import com.haru.api.domain.user.repository.UserRepository;
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

    @Transactional(readOnly = true)
    @Override
    public List<WorkspaceResponseDTO.Document> getDocuments(Long userId, Long workspaceId, String title) {

        userRepository.findById(userId)
                .orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));

        workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceHandler(ErrorStatus.WORKSPACE_NOT_FOUND));

        List<WorkspaceResponseDTO.Document> documents = Stream.of(
                meetingRepository.findDocumentsByTitleLike(title),
                snsEventRepository.findDocumentsByTitleLike(title),
                moodTrackerRepository.findDocumentsByTitleLike(title)
        ).flatMap(List::stream)
        .toList();

        for(WorkspaceResponseDTO.Document document : documents) {
            System.out.print("title: " + document.getTitle());
            System.out.print(", Id: " + document.getDocumentId());
            System.out.print(", type: " + document.getDocumentType());
        }

        return documents;
    }
}
