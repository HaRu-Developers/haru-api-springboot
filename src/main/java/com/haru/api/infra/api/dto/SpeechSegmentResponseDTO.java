package com.haru.api.infra.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class SpeechSegmentResponseDTO {

    @Getter
    @Builder
    public static class SpeechSegmentResponse {
        private String speakerId;
        private String text;
        private LocalDateTime startTime;
    }
}
