package com.haru.api.domain.term.dto;

import com.haru.api.domain.term.entity.enums.TermType;
import lombok.Builder;
import lombok.Getter;

public class TermResponseDTO {

    @Getter
    @Builder
    public static class TermDetail {
        TermType type;
        String title;
        String content;
    }
}
