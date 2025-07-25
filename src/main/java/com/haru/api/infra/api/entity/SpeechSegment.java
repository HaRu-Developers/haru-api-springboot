package com.haru.api.infra.api.entity;

import com.haru.api.domain.meeting.entity.Meeting;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SpeechSegment {
    private Long id;
    private String speakerId;
    private String text;
    private LocalDateTime startTime;
    private Meeting meeting;

    @Override
    public String toString() {
        return String.format("Speaker %s said: %s (start at %s)",
                speakerId, text, startTime);
    }
}
