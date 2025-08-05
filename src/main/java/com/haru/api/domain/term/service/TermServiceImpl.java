package com.haru.api.domain.term.service;

import com.haru.api.domain.term.converter.TermConverter;
import com.haru.api.domain.term.dto.TermResponseDTO;
import com.haru.api.domain.term.entity.Term;
import com.haru.api.domain.term.entity.enums.TermType;
import com.haru.api.domain.term.repository.TermRepository;
import com.haru.api.global.apiPayload.code.status.ErrorStatus;
import com.haru.api.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TermServiceImpl implements TermService {

    private final TermRepository termsRepository;

    @Override
    @Transactional(readOnly = true)
    public TermResponseDTO.TermDetail getTermByType(TermType type) {
        Term term = termsRepository.findByType(type)
                .orElseThrow(() -> new GeneralException(ErrorStatus.TERM_NOT_FOUND));
        return TermConverter.toTermsDetailDTO(term);
    }
}
