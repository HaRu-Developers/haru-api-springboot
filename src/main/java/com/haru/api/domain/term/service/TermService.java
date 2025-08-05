package com.haru.api.domain.term.service;

import com.haru.api.domain.term.dto.TermResponseDTO;
import com.haru.api.domain.term.entity.enums.TermType;

public interface TermService {
    TermResponseDTO.TermDetail getTermByType(TermType type);
}
