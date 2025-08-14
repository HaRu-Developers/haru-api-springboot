package com.haru.api.infra.api.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class SpeechSegmentResponseDTO {

    @Getter
    @Builder
    public static class SpeechSegmentResponse {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long speechId;
        private String speakerId;
        private String text;
        private LocalDateTime startTime;
    }
}
