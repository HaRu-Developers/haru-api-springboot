package com.haru.api.domain.meeting.service;

import com.haru.api.domain.meeting.dto.MeetingResponseDTO;

import java.util.List;

public interface MeetingQueryService {

    List<MeetingResponseDTO.getMeetingResponse> getMeetings(Long userId, Long workspaceId);

    MeetingResponseDTO.getMeetingProceeding getMeetingProceeding(Long userId, Long meetingId);

    MeetingResponseDTO.TranscriptResponse getTranscript(Long userId, Long meetingId);

    MeetingResponseDTO.proceedingDownLoadLinkResponse downloadMeeting(Long userId, Long meetingId);
}
