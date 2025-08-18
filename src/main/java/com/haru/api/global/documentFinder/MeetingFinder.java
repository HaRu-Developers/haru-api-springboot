package com.haru.api.global.documentFinder;

import com.haru.api.domain.lastOpened.entity.enums.DocumentType;
import com.haru.api.domain.meeting.repository.MeetingRepository;
import com.haru.api.global.apiPayload.code.status.ErrorStatus;
import com.haru.api.global.apiPayload.exception.handler.MeetingHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MeetingFinder implements DocumentFinder{

    private final MeetingRepository meetingRepository;

    @Override
    public DocumentType getSupportType() {
        return DocumentType.AI_MEETING_MANAGER;
    }

    @Override
    public Object findById(Object id) {
        return meetingRepository.findById((Long)id)
                .orElseThrow(() -> new MeetingHandler(ErrorStatus.MEETING_NOT_FOUND));
    }
}
