package com.haru.api.domain.meeting.dto;

import com.haru.api.domain.meeting.entity.Meetings;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;


public class MeetingResponseDTO {
    @Getter
    @Builder
    public static class createMeetingResponse{
        private String title;
        private Long meetingId;
        private LocalDateTime updatedAt;

    }
}
