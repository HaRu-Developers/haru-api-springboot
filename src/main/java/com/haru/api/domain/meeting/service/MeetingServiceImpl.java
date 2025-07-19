package com.haru.api.domain.meeting.service;

import com.haru.api.domain.meeting.converter.MeetingConverter;
import com.haru.api.domain.meeting.dto.MeetingRequestDTO;
import com.haru.api.domain.meeting.dto.MeetingResponseDTO;
import com.haru.api.domain.meeting.entity.Meetings;
import com.haru.api.domain.meeting.repository.MeetingRepository;
import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.user.repository.UserRepository;
import com.haru.api.domain.workspace.entity.Workspace;
import com.haru.api.domain.workspace.repository.WorkspaceRepository;
import com.haru.api.global.apiPayload.code.status.ErrorStatus;
import com.haru.api.global.apiPayload.exception.handler.MemberHandler;
import com.haru.api.global.apiPayload.exception.handler.WorkspaceHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingServiceImpl implements MeetingService{

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final MeetingRepository meetingRepository;

    @Override
    @Transactional
    public MeetingResponseDTO.createMeetingResponse createMeeting(
            Long userId,
            MultipartFile agendaFile,
            MeetingRequestDTO.createMeetingRequest request)
    {
        User foundUser = userRepository.findById(userId)
                .orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));

        Workspace foundWorkspace = workspaceRepository.findById(request.getWorkspaceId())
                .orElseThrow(() -> new WorkspaceHandler(ErrorStatus.WORKSPACE_NOT_FOUND));

        // agendaFile을 openAi 활용하여 요약 - 미구현
        String agendaResult = "안건지 요약 - 미구현";


        Meetings newMeetings = Meetings.createInitialMeeting(
                request.getTitle(),
                agendaResult,
                foundUser,
                foundWorkspace
        );

        Meetings savedMeeting = meetingRepository.save(newMeetings);


        return MeetingConverter.toCreateMeetingResponse(savedMeeting);
    }

    @Override
    public List<MeetingResponseDTO.getMeetingResponse> getMeetings(Long userId, Long workspaceId) {
        User foundUser = userRepository.findById(userId)
                .orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));

        Workspace foundWorkspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceHandler(ErrorStatus.WORKSPACE_NOT_FOUND));

        List<Meetings> foundMeetings = meetingRepository.findByWorkspaces(foundWorkspace);

        return foundMeetings.stream()
                .map(meeting -> MeetingConverter.toGetMeetingResponse(meeting, userId))
                .collect(Collectors.toList());
    }
}
