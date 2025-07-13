package com.haru.api.domain.meeting.converter;

import com.haru.api.domain.meeting.dto.MeetingResponseDTO;
import com.haru.api.domain.meeting.entity.Meetings;
import org.springframework.stereotype.Component;

@Component
public class MeetingConverter {

    // Entity -> ResponseDTO
    public static MeetingResponseDTO.createMeetingResponse toCreateMeetingResponse(Meetings meeting) {
        return MeetingResponseDTO.createMeetingResponse.builder()
                .meetingId(meeting.getId())
                .title(meeting.getTitle())
                .updatedAt(meeting.getCreatedAt())
                .build();
    }
}
