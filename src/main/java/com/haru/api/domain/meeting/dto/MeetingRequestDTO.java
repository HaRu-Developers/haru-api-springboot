package com.haru.api.domain.meeting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class MeetingRequestDTO {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class createMeetingRequest{
        private Long workspaceId;
        private String title;
    }
    @Getter
    public static class updateTitle {
        private String title;
    }

    @Getter
    public static class meetingProceedingRequest{
        private String proceeding;
    }
}
