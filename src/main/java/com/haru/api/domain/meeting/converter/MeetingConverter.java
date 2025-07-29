package com.haru.api.domain.meeting.converter;

import com.haru.api.domain.meeting.dto.MeetingResponseDTO;
import com.haru.api.domain.meeting.entity.Meeting;
import org.springframework.stereotype.Component;

@Component
public class MeetingConverter {

    // Entity -> ResponseDTO
    public static MeetingResponseDTO.createMeetingResponse toCreateMeetingResponse(Meeting meeting) {
        return MeetingResponseDTO.createMeetingResponse.builder()
                .meetingId(meeting.getId())
                .title(meeting.getTitle())
                .build();
    }

    public static MeetingResponseDTO.getMeetingResponse toGetMeetingResponse(Meeting meeting, Long requesterId) {

        boolean isCreator = meeting.getCreator().getId().equals(requesterId);

        return MeetingResponseDTO.getMeetingResponse.builder()
                .meetingId(meeting.getId())
                .title(meeting.getTitle())
                .isCreator(isCreator)
                .updatedAt(meeting.getUpdatedAt())
                .build();
    }

}
