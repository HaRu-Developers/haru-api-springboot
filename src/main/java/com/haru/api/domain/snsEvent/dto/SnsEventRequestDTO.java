package com.haru.api.domain.snsEvent.dto;

import lombok.*;

import java.time.LocalDateTime;

public class SnsEventRequestDTO {
    @Getter
    @Setter
    @NoArgsConstructor
    public static class CreateSnsRequest {
        private String title;
        private String snsEventLink;
        private SnsCondition snsCondition;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
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
