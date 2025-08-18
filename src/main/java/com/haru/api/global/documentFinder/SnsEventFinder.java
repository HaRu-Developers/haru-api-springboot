package com.haru.api.global.documentFinder;

import com.haru.api.domain.lastOpened.entity.enums.DocumentType;
import com.haru.api.domain.snsEvent.repository.SnsEventRepository;
import com.haru.api.global.apiPayload.code.status.ErrorStatus;
import com.haru.api.global.apiPayload.exception.handler.SnsEventHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SnsEventFinder implements DocumentFinder {

    private final SnsEventRepository snsEventRepository;

    @Override
    public DocumentType getSupportType() {
        return DocumentType.SNS_EVENT_ASSISTANT;
    }

    @Override
    public Object findById(Object id) {
        return snsEventRepository.findById((Long) id)
                .orElseThrow(() -> new SnsEventHandler(ErrorStatus.SNS_EVENT_NOT_FOUND));
    }
}
