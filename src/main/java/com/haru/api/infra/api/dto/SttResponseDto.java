package com.haru.api.infra.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SttResponseDto {
    private String message;

    @JsonProperty("by_speaker")
    private Map<String, SpeakerUtterance> bySpeaker;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpeakerUtterance {
        private String text;
        private double start; // 단위: sec
    }
}
