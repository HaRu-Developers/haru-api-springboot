package com.haru.api.domain.workspace.service;

import com.haru.api.domain.lastOpened.entity.UserDocumentLastOpened;
import com.haru.api.domain.lastOpened.repository.UserDocumentLastOpenedRepository;
import com.haru.api.domain.meeting.entity.Meeting;
import com.haru.api.domain.meeting.repository.MeetingRepository;
import com.haru.api.domain.moodTracker.entity.MoodTracker;
import com.haru.api.domain.moodTracker.repository.MoodTrackerRepository;
import com.haru.api.domain.snsEvent.entity.SnsEvent;
import com.haru.api.domain.snsEvent.repository.SnsEventRepository;
import com.haru.api.domain.user.converter.UserConverter;
import com.haru.api.domain.user.dto.UserResponseDTO;
import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.userWorkspace.repository.UserWorkspaceRepository;
import com.haru.api.domain.workspace.converter.WorkspaceConverter;
import com.haru.api.domain.workspace.dto.WorkspaceResponseDTO;
import com.haru.api.domain.workspace.entity.Workspace;
import com.haru.api.domain.workspace.repository.WorkspaceRepository;
import com.haru.api.global.apiPayload.code.status.ErrorStatus;
import com.haru.api.global.apiPayload.exception.handler.*;
import com.haru.api.infra.s3.AmazonS3Manager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkspaceQueryServiceImpl implements WorkspaceQueryService {

    private final MeetingRepository meetingRepository;
    private final SnsEventRepository snsEventRepository;
    private final MoodTrackerRepository moodTrackerRepository;
    private final UserWorkspaceRepository userWorkspaceRepository;
    private final UserDocumentLastOpenedRepository userDocumentLastOpenedRepository;
    private final WorkspaceConverter workspaceConverter;
    private final AmazonS3Manager amazonS3Manager;

    @Override
    public WorkspaceResponseDTO.DocumentList getDocuments(User user, Workspace workspace, String title) {

        List<UserDocumentLastOpened> documentList = userDocumentLastOpenedRepository.findRecentDocumentsByTitle(workspace.getId(), user.getId(), title);

        return WorkspaceConverter.toDocumentList(
                documentList.stream()
                        .map(workspaceConverter::toDocument)
                        .toList()
        );
    }

    @Override
    public WorkspaceResponseDTO.DocumentSidebarList getDocumentWithoutLastOpenedList(User user, Workspace workspace) {

        // 유저가 가장 최근에 조회한 문서 5개 추출
        List<UserDocumentLastOpened> documentList = userDocumentLastOpenedRepository.findTop5ByWorkspaceIdAndUserIdOrderByLastOpenedDesc(workspace.getId(), user.getId());

        return WorkspaceConverter.toDocumentSidebarList(
                documentList.stream()
                        .map(workspaceConverter::toDocumentSidebar)
                        .toList()
        );
    }

    @Override
    public WorkspaceResponseDTO.DocumentCalendarList getDocumentForCalendar(User user, Workspace workspace, LocalDate startDate, LocalDate endDate) {

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // 워크스페이스에 속하면서 생성 날짜가 startDate, endDate 사이인 문서 리스트 검색
        List<Meeting> meetingList = meetingRepository.findAllDocumentForCalendars(workspace, startDateTime, endDateTime);
        List<SnsEvent> snsEventList = snsEventRepository.findAllDocumentForCalendars(workspace, startDateTime, endDateTime);
        List<MoodTracker> moodTrackerList = moodTrackerRepository.findAllDocumentForCalendars(workspace, startDateTime, endDateTime);

        // 모든 문서 합치기
        return workspaceConverter.toDocumentCalendarList(meetingList, snsEventList, moodTrackerList);
    }

    @Override
    public WorkspaceResponseDTO.WorkspaceEditPage getEditPage(User user, Workspace workspace) {

        List<UserResponseDTO.MemberInfo> memberInfoList = userWorkspaceRepository.findUsersByWorkspace(workspace).stream()
                .map(UserConverter::toMemberInfo)
                .toList();

        String imageUrl = amazonS3Manager.generatePresignedUrl(foundWorkspace.getKeyName());

        return workspaceConverter.toWorkspaceEditPage(foundWorkspace, memberInfoList, imageUrl);
    }
}
