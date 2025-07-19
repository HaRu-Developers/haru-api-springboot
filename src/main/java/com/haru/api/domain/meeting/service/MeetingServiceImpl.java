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
import com.haru.api.global.apiPayload.exception.handler.MeetingHandler;
import com.haru.api.global.apiPayload.exception.handler.MemberHandler;
import com.haru.api.global.apiPayload.exception.handler.TempHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
                .orElseThrow(() -> new TempHandler(ErrorStatus.WORKSPACE_NOT_FOUND));

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
    @Transactional
    public void updateMeetingTitle(Long userId, Long meetingId, String newTitle) {

        Meetings meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new MeetingHandler(ErrorStatus.MEETING_NOT_FOUND));

        User foundUser = userRepository.findById(userId)
                .orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));

        // 회의 생성자 권한 확인
        if (!meeting.getUser().getId().equals(userId)) {
            throw new MemberHandler(ErrorStatus.MEMBER_NO_AUTHORITY);
        }

        meeting.updateTitle(newTitle);

    }
}
