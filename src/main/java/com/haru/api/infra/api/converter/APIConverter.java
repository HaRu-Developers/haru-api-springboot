package com.haru.api.infra.api.converter;

import com.haru.api.infra.api.dto.SttResponseDto;
import com.haru.api.infra.api.entity.SpeechSegment;

import java.time.LocalDateTime;

public class APIConverter {

    public static SpeechSegment toSpeechSegment(
            String speakerId,
            SttResponseDto.SpeakerUtterance utterance,
            LocalDateTime baseStartTime
    ) {
        double startSeconds = utterance.getStart();
        long nanosToAdd = (long) (startSeconds * 1_000_000_000L);
        LocalDateTime startAt = baseStartTime.plusNanos(nanosToAdd);

        return SpeechSegment.builder()
                .speakerId(speakerId)
                .text(utterance.getText())
                .startTime(startAt)
                .build();
    }
}
