package com.haru.api.domain.meeting.dto;

import lombok.Builder;
import lombok.Getter;

public class MeetingRequestDTO {

    @Getter
    @Builder
    public static class createMeetingRequest{
        private Long workspaceId;
        private String title;
    }
}
