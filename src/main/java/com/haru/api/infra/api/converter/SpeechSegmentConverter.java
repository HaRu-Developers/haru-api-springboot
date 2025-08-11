package com.haru.api.infra.api.converter;

import com.haru.api.domain.meeting.entity.Meeting;
import com.haru.api.infra.api.dto.SpeechSegmentResponseDTO;
import com.haru.api.infra.api.dto.SttResponseDTO;
import com.haru.api.infra.api.entity.SpeechSegment;

import java.time.LocalDateTime;

public class SpeechSegmentConverter {

    public static SpeechSegment toSpeechSegment(
            SttResponseDTO.UtteranceDTO utteranceDto,
            Meeting meeting,
            LocalDateTime baseStartTime
    ) {
        double startSeconds = utteranceDto.getStart();
        long nanosToAdd = (long) (startSeconds * 1_000_000_000L);
        LocalDateTime startAt = baseStartTime.plusNanos(nanosToAdd);

        return SpeechSegment.builder()
                .speakerId(utteranceDto.getSpeakerId())
                .text(utteranceDto.getText())
                .meeting(meeting)
                .startTime(startAt)
                .build();
    }

    public static SpeechSegmentResponseDTO.SpeechSegmentResponse toSpeechSegmentResponseDTO(SpeechSegment speechSegment) {
        return SpeechSegmentResponseDTO.SpeechSegmentResponse.builder()
                .speechId(speechSegment.getId())
                .text(speechSegment.getText())
                .startTime(speechSegment.getStartTime())
                .build();
    }
}
