package com.haru.api.domain.meeting.service;

import com.haru.api.domain.meeting.dto.MeetingRequestDTO;
import com.haru.api.domain.meeting.dto.MeetingResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MeetingCommandService {

    public MeetingResponseDTO.createMeetingResponse createMeeting(Long userId, MultipartFile agendaFile, MeetingRequestDTO.createMeetingRequest request);
    public void updateMeetingTitle(Long userId, String meetingId, String newTitle);
    public void deleteMeeting(Long userId, String meetingId);
    public void adjustProceeding(Long userId, String meetingId, MeetingRequestDTO.meetingProceedingRequest newProceeding);

}
