package com.haru.api.domain.meeting.service;

import com.haru.api.domain.meeting.dto.MeetingRequestDTO;
import com.haru.api.domain.meeting.dto.MeetingResponseDTO;
import org.springframework.web.multipart.MultipartFile;

public interface MeetingCommandService {

    MeetingResponseDTO.createMeetingResponse createMeeting(Long userId, MultipartFile agendaFile, MeetingRequestDTO.createMeetingRequest request);
    void updateMeetingTitle(Long userId, Long meetingId, String newTitle);
    void deleteMeeting(Long userId, Long meetingId);
    void adjustProceeding(Long userId, Long meetingId, MeetingRequestDTO.meetingProceedingRequest newProceeding);

}
