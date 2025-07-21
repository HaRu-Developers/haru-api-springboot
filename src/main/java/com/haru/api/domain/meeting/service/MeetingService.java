package com.haru.api.domain.meeting.service;

import com.haru.api.domain.meeting.dto.MeetingRequestDTO;
import com.haru.api.domain.meeting.dto.MeetingResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MeetingService {

    public MeetingResponseDTO.createMeetingResponse createMeeting(Long userId, MultipartFile agendaFile, MeetingRequestDTO.createMeetingRequest request);
    public void updateMeetingTitle(Long userId, Long meetingId, String newTitle);
    public void deleteMeeting(Long userId, Long meetingId);

    public List<MeetingResponseDTO.getMeetingResponse> getMeetings(Long userId, Long meetingId);

}
