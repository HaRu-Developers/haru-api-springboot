package com.haru.api.infra.api.converter;

import com.haru.api.domain.meeting.entity.Meeting;
import com.haru.api.infra.api.dto.SpeechSegmentResponseDTO;
import com.haru.api.infra.api.dto.SttResponseDto;
import com.haru.api.infra.api.entity.SpeechSegment;

import java.time.LocalDateTime;

public class SpeechSegmentConverter {

    public static SpeechSegment toSpeechSegment(
            String speakerId,
            SttResponseDto.SpeakerUtterance utterance,
            Meeting meeting,
            LocalDateTime baseStartTime
    ) {
        double startSeconds = utterance.getStart();
        long nanosToAdd = (long) (startSeconds * 1_000_000_000L);
        LocalDateTime startAt = baseStartTime.plusNanos(nanosToAdd);

        return SpeechSegment.builder()
                .speakerId(speakerId)
                .text(utterance.getText())
                .meeting(meeting)
                .startTime(startAt)
                .build();
    }

    public static SpeechSegmentResponseDTO.SpeechSegmentResponse toSpeechSegmentResponseDTO(SpeechSegment speechSegment) {
        return SpeechSegmentResponseDTO.SpeechSegmentResponse.builder()
                .speakerId(speechSegment.getSpeakerId())
                .text(speechSegment.getText())
                .startTime(speechSegment.getStartTime())
                .build();
    }
}
