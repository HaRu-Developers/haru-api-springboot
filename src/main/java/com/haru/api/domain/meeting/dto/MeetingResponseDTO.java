package com.haru.api.domain.meeting.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;


public class MeetingResponseDTO {
    @Getter
    @Builder
    public static class createMeetingResponse{
        @JsonSerialize(using = ToStringSerializer.class)
        private Long meetingId;
        private String title;
    }

    @Getter
    @Builder
    public static class getMeetingResponse{
        @JsonSerialize(using = ToStringSerializer.class)
        private Long meetingId;
        private String title;
        private boolean isCreator;
        private LocalDateTime updatedAt;
    }

    @Getter
    @Builder
    public static class getMeetingProceeding{
        @JsonSerialize(using = ToStringSerializer.class)
        private Long userId;
        private String email;
        private String userName;
        private String title;
        private String proceeding;
        private LocalDateTime updatedAt;
    }
}
