package com.haru.api.domain.workspace.service;

import com.haru.api.domain.lastOpened.entity.UserDocumentLastOpened;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

        // workspace에 속해있는 각 문서별로 title이 일치하는 문서 검색
        List<WorkspaceResponseDTO.Document> meetingList = meetingRepository.findRecentDocumentsByTitle(workspaceId, foundUser.getId(), title);
        List<WorkspaceResponseDTO.Document> snsEventList = snsEventRepository.findRecentDocumentsByTitle(workspaceId, foundUser.getId(), title);
        List<WorkspaceResponseDTO.Document> moodTrackerList = moodTrackerRepository.findRecentDocumentsByTitle(workspaceId, foundUser.getId(), title);

        // 모든 문서 리스트 스트림으로 합침
        List<WorkspaceResponseDTO.Document> allResults = new ArrayList<>();
        allResults.addAll(meetingList);
        allResults.addAll(snsEventList);
        allResults.addAll(moodTrackerList);

        // last_opened가 null인 경우에는 가장 뒤로 보내기
        // last_opened가 모두 null인 경우에도 동작하도록 구현
        List<WorkspaceResponseDTO.Document> finalResult = allResults.stream()
                .sorted(Comparator.comparing(WorkspaceResponseDTO.Document::getLastOpened,
                                            Comparator.nullsLast(Comparator.naturalOrder()))
                                    .reversed())
                .toList();

        return WorkspaceConverter.toDocumentsDTO(finalResult);

    }

    @Transactional(readOnly = true)
    @Override
    public WorkspaceResponseDTO.DocumentSidebarList getDocumentWithoutLastOpenedList(Long userId, Long workspaceId) {

        // 유저 존재 확인
        User foundUser = userRepository.findById(userId)
                .orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));

        // workspace 존재 확인
        workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceHandler(ErrorStatus.WORKSPACE_NOT_FOUND));

        // 유저가 워크스페이스에 속해있는지 확인
        if (!userWorkspaceRepository.existsByWorkspaceIdAndUserId(workspaceId, userId))
            throw new WorkspaceHandler(ErrorStatus.NOT_BELONG_TO_WORKSPACE);

        // 유저가 가장 최근에 조회한 문서 5개 추출
        List<UserDocumentLastOpened> lastOpenedList = userDocumentLastOpenedRepository.findTop5ByWorkspaceIdAndUserIdOrderByLastOpenedDesc(workspaceId, userId);

        return WorkspaceConverter.toDocumentSidebarList(
                lastOpenedList.stream()
                        .map(WorkspaceConverter::toDocumentSidebar)
                        .toList()
        );
    }

    @Override
    public WorkspaceResponseDTO.DocumentCalendarList getDocumentForCalendar(Long userId, Long workspaceId, LocalDate startDate, LocalDate endDate) {

        // 유저 존재 확인
        User foundUser = userRepository.findById(userId)
                .orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));

        // workspace 존재 확인
        workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceHandler(ErrorStatus.WORKSPACE_NOT_FOUND));

        // 유저가 워크스페이스에 속해있는지 확인
        if (!userWorkspaceRepository.existsByWorkspaceIdAndUserId(workspaceId, userId))
            throw new WorkspaceHandler(ErrorStatus.NOT_BELONG_TO_WORKSPACE);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // 워크스페이스에 속하면서 생성 날짜가 startDate, endDate 사이인 문서 리스트 검색
        List<WorkspaceResponseDTO.DocumentCalendar> meetingList = meetingRepository.findAllDocumentForCalendars(workspaceId, startDateTime, endDateTime);
        List<WorkspaceResponseDTO.DocumentCalendar> snsEventList = snsEventRepository.findAllDocumentForCalendars(workspaceId, startDateTime, endDateTime);
        List<WorkspaceResponseDTO.DocumentCalendar> moodTrackerList = moodTrackerRepository.findAllDocumentForCalendars(workspaceId, startDateTime, endDateTime);

        // 모든 문서 합치기
        List<WorkspaceResponseDTO.DocumentCalendar> allResults = new ArrayList<>();
        allResults.addAll(meetingList);
        allResults.addAll(snsEventList);
        allResults.addAll(moodTrackerList);

        return WorkspaceConverter.toDocumentCalendarList(allResults);
    }
}
