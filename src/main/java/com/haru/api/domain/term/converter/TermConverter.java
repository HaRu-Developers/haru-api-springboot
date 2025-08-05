package com.haru.api.domain.term.converter;

import com.haru.api.domain.term.dto.TermResponseDTO;
import com.haru.api.domain.term.entity.Term;
import org.springframework.stereotype.Component;

@Component
public class TermConverter {

    public static TermResponseDTO.TermDetail toTermsDetailDTO(Term term) {
        return TermResponseDTO.TermDetail.builder()
                .type(term.getType())
                .title(term.getTitle())
                .content(term.getContent())
                .build();
    }
}
