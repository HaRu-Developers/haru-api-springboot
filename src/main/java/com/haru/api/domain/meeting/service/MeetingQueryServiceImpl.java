package com.haru.api.domain.meeting.service;

import com.haru.api.domain.lastOpened.entity.enums.DocumentType;
import com.haru.api.domain.meeting.converter.MeetingConverter;
import com.haru.api.domain.meeting.dto.MeetingResponseDTO;
import com.haru.api.domain.meeting.entity.Meeting;
import com.haru.api.domain.meeting.repository.MeetingRepository;
import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.user.repository.UserRepository;
import com.haru.api.domain.userWorkspace.entity.UserWorkspace;
import com.haru.api.domain.userWorkspace.repository.UserWorkspaceRepository;
import com.haru.api.domain.workspace.entity.Workspace;
import com.haru.api.domain.workspace.repository.WorkspaceRepository;
import com.haru.api.global.annotation.TrackLastOpened;
import com.haru.api.global.apiPayload.code.status.ErrorStatus;
import com.haru.api.global.apiPayload.exception.handler.MeetingHandler;
import com.haru.api.global.apiPayload.exception.handler.MemberHandler;
import com.haru.api.global.apiPayload.exception.handler.UserWorkspaceHandler;
import com.haru.api.global.apiPayload.exception.handler.WorkspaceHandler;
import com.haru.api.infra.s3.AmazonS3Manager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingQueryServiceImpl implements MeetingQueryService{

    private final MeetingRepository meetingRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final UserWorkspaceRepository userWorkspaceRepository;
    private final AmazonS3Manager amazonS3Manager;

    @Override
    public List<MeetingResponseDTO.getMeetingResponse> getMeetings(Long userId, Long workspaceId) {
        User foundUser = userRepository.findById(userId)
                .orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));

        Workspace foundWorkspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceHandler(ErrorStatus.WORKSPACE_NOT_FOUND));

        List<Meeting> foundMeetings = meetingRepository.findByWorkspaceOrderByUpdatedAtDesc(foundWorkspace);

        return foundMeetings.stream()
                .map(meeting -> MeetingConverter.toGetMeetingResponse(meeting, userId))
                .collect(Collectors.toList());
    }

    @Override
    @TrackLastOpened(type = DocumentType.AI_MEETING_MANAGER)
    public MeetingResponseDTO.getMeetingProceeding getMeetingProceeding(Long userId, Long meetingId){
        User foundUser = userRepository.findById(userId)
                .orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));

        Meeting foundMeeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new MeetingHandler(ErrorStatus.MEETING_NOT_FOUND));

        Workspace foundWorkspace = meetingRepository.findWorkspaceByMeetingId(meetingId)
                .orElseThrow(() -> new WorkspaceHandler(ErrorStatus.WORKSPACE_NOT_FOUND));

        UserWorkspace foundUserWorkspace = userWorkspaceRepository.findByUserIdAndWorkspaceId(userId, foundWorkspace.getId())
                .orElseThrow(() -> new UserWorkspaceHandler(ErrorStatus.USER_WORKSPACE_NOT_FOUND));

        User foundMeetingCreator = foundMeeting.getCreator();

        return MeetingConverter.toGetMeetingProceedingResponse(foundMeetingCreator, foundMeeting);
    }

    @Override
    public MeetingResponseDTO.proceedingDownLoadLinkResponse downloadMeeting(Long userId, Long meetingId){
        User foundUser = userRepository.findById(userId)
                .orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));

        Meeting foundMeeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new MeetingHandler(ErrorStatus.MEETING_NOT_FOUND));

        Workspace foundWorkspace = meetingRepository.findWorkspaceByMeetingId(meetingId)
                .orElseThrow(() -> new WorkspaceHandler(ErrorStatus.WORKSPACE_NOT_FOUND));

        UserWorkspace foundUserWorkspace = userWorkspaceRepository.findByUserIdAndWorkspaceId(userId, foundWorkspace.getId())
                .orElseThrow(() -> new UserWorkspaceHandler(ErrorStatus.USER_WORKSPACE_NOT_FOUND));

        String proceedingKeyName = foundMeeting.getProceedingKeyName();

        if (proceedingKeyName == null || proceedingKeyName.isBlank()) {
            throw new MeetingHandler(ErrorStatus.MEETING_PROCEEDING_NOT_FOUND);
        }

        String presignedUrl = amazonS3Manager.generatePresignedUrl(proceedingKeyName);

        return MeetingResponseDTO.proceedingDownLoadLinkResponse.builder()
                .downloadLink(presignedUrl)
                .build();
    }
}
