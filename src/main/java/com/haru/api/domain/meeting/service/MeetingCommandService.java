package com.haru.api.domain.meeting.service;

import com.haru.api.domain.meeting.dto.MeetingRequestDTO;
import com.haru.api.domain.meeting.dto.MeetingResponseDTO;
import com.haru.api.domain.meeting.entity.Meeting;
import com.haru.api.domain.user.entity.User;
import com.haru.api.infra.websocket.AudioSessionBuffer;
import org.springframework.web.multipart.MultipartFile;

public interface MeetingCommandService {

    MeetingResponseDTO.createMeetingResponse createMeeting(User user, MultipartFile agendaFile, MeetingRequestDTO.createMeetingRequest request);

    void updateMeetingTitle(User user, Meeting meeting, MeetingRequestDTO.updateTitle request);

    void deleteMeeting(User user, Meeting meeting);

    void adjustProceeding(User user, Meeting meeting, MeetingRequestDTO.meetingProceedingRequest newProceeding);

    void endMeeting(User user, Meeting meeting);

    void processAfterMeeting(AudioSessionBuffer audioSessionBuffer);


}
