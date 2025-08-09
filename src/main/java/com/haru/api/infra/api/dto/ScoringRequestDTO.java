package com.haru.api.infra.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ScoringRequestDTO {
    @JsonProperty("speech_id")
    private Long speechId;

    private String utterance;

    @JsonProperty("has_agenda")
    private Boolean hasAgenda;

    @JsonProperty("agenda_text")
    private String agendaText;

    @JsonProperty("recent_utterances")
    private List<String> recentUtterances;
}
