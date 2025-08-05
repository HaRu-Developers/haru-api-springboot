package com.haru.api.domain.meeting.service;

import com.haru.api.domain.meeting.dto.MeetingResponseDTO;

import java.util.List;

public interface MeetingQueryService {

    public List<MeetingResponseDTO.getMeetingResponse> getMeetings(Long userId, String workspaceId);

    public MeetingResponseDTO.getMeetingProceeding getMeetingProceeding(Long userId, String meetingId);

}
