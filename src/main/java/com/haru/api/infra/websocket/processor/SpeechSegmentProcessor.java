package com.haru.api.infra.websocket.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.haru.api.infra.api.converter.SpeechSegmentConverter;
import com.haru.api.infra.api.dto.SttResponseDTO;
import com.haru.api.infra.api.entity.SpeechSegment;
import com.haru.api.infra.api.repository.SpeechSegmentRepository;
import com.haru.api.infra.websocket.AudioSessionBuffer;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class SpeechSegmentProcessor {
    private final AudioSessionBuffer audioSessionBuffer;
    private final SpeechSegmentRepository speechSegmentRepository;
    private final WebSocketNotificationService notificationService;
    private final ObjectMapper objectMapper;

    public SpeechSegmentProcessor(AudioSessionBuffer audioSessionBuffer,
                                  SpeechSegmentRepository speechSegmentRepository,
                                  WebSocketNotificationService notificationService,
                                  ObjectMapper objectMapper) {
        this.audioSessionBuffer = audioSessionBuffer;
        this.speechSegmentRepository = speechSegmentRepository;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    public Mono<List<SpeechSegment>> processSttResult(String sttResult) {
        return Mono.fromCallable(() -> {
            SttResponseDTO sttResponse = objectMapper.readValue(sttResult, SttResponseDTO.class);

            return sttResponse.getUtterances().stream()
                    .map(this::createAndSaveSpeechSegment)
                    .collect(Collectors.toList());
        });
    }

    private SpeechSegment createAndSaveSpeechSegment(SttResponseDTO.UtteranceDTO utteranceDto) {
        SpeechSegment segment = SpeechSegmentConverter.toSpeechSegment(
                utteranceDto,
                audioSessionBuffer.getMeeting(),
                audioSessionBuffer.getUtteranceStartTime()
        );

        log.info("Speaker {} said: {} (start at {})",
                segment.getSpeakerId(), segment.getText(), segment.getStartTime());

        // 버퍼에 추가 및 DB 저장
        audioSessionBuffer.putUtterance(segment);
        SpeechSegment savedSegment = speechSegmentRepository.save(segment);

        // 클라이언트에게 알림
        notificationService.sendUtteranceNotification(segment);

        return savedSegment;
    }
}
