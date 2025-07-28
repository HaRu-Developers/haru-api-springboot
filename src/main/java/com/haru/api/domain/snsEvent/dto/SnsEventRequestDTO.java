package com.haru.api.domain.snsEvent.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class SnsEventRequestDTO {
    @Getter
    @Builder
    public static class CreateSnsRequest {
        private String title;
        private String snsEventLink;
        private SnsCondition snsCondition;
    }

    @Getter
    @Builder
    public static class SnsCondition {
        private Integer winnerCount;
        private Boolean isPeriod;
        private LocalDateTime period;
        private Boolean isKeyword;
        private String keyword;
        private Boolean isTaged;
        private Integer tageCount;
    }
}
