package com.haru.api.infra.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SttResponseDTO {
    private String message;

    @JsonProperty("utterances")
    private List<UtteranceDTO> utterances; // 필드 이름과 타입 변경

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UtteranceDTO {
        @JsonProperty("speaker_id") // JSON 키 이름과 일치하도록 추가
        private String speakerId;
        private String text;
        private double start; // 단위: sec
    }
}
