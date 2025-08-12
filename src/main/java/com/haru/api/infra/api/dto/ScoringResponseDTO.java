package com.haru.api.infra.api.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScoringResponseDTO {
    private Long speechId;
    private Double score;
    private Boolean isQuestionNeeded;
}
