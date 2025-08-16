package com.haru.api.domain.meeting.service;

import com.haru.api.domain.meeting.dto.MeetingResponseDTO;
import com.haru.api.domain.meeting.entity.Meeting;
import com.haru.api.domain.user.entity.User;

import java.util.List;

public interface MeetingQueryService {

    List<MeetingResponseDTO.getMeetingResponse> getMeetings(User user, Meeting meeting);

    MeetingResponseDTO.getMeetingProceeding getMeetingProceeding(User user, Meeting meeting);

    MeetingResponseDTO.TranscriptResponse getTranscript(User user, Meeting meeting);

    MeetingResponseDTO.proceedingDownLoadLinkResponse downloadMeeting(User user, Meeting meeting);

    MeetingResponseDTO.proceedingVoiceLinkResponse getMeetingVoiceFile(User user, Meeting meeting);
}
