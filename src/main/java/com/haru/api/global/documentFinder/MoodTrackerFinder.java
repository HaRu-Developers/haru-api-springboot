package com.haru.api.global.documentFinder;

import com.haru.api.domain.lastOpened.entity.enums.DocumentType;
import com.haru.api.domain.moodTracker.repository.MoodTrackerRepository;
import com.haru.api.global.apiPayload.code.status.ErrorStatus;
import com.haru.api.global.apiPayload.exception.handler.MoodTrackerHandler;
import com.haru.api.global.util.HashIdUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MoodTrackerFinder implements DocumentFinder{

    private final MoodTrackerRepository moodTrackerRepository;
    private final HashIdUtil hashIdUtil;

    @Override
    public DocumentType getSupportType() {
        return DocumentType.TEAM_MOOD_TRACKER;
    }

    @Override
    public Object findById(Object id) {
        Long decodedId = hashIdUtil.decode((String) id);
        return moodTrackerRepository.findById(decodedId)
                .orElseThrow(() -> new MoodTrackerHandler(ErrorStatus.MOOD_TRACKER_NOT_FOUND));
    }
}
