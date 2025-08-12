package com.haru.api.infra.websocket.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haru.api.infra.api.converter.SpeechSegmentConverter;
import com.haru.api.infra.api.dto.SttResponseDTO;
import com.haru.api.infra.api.entity.SpeechSegment;
import com.haru.api.infra.api.repository.SpeechSegmentRepository;
import com.haru.api.infra.websocket.AudioSessionBuffer;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

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


    public Flux<SpeechSegment> processSttResult(String sttResult) {
        // FastAPI의 STT API의 결과를 JSON -> SttResponseDTO로 변환
        return Mono.fromCallable(() -> objectMapper.readValue(sttResult, SttResponseDTO.class))
                .doOnError(error -> log.error("STT 결과 JSON 파싱 실패. Raw JSON: {}", sttResult, error))
                .onErrorResume(JsonProcessingException.class, error -> Mono.empty())
                .flatMapMany(sttResponse -> {
                    // 한 개의 SttResponseDTO에 여러 개의 발화가 있을 수도 있음
                    // 각 발화에 대해 createAndSaveSpeechSegment 메서드 적용
                    List<SttResponseDTO.UtteranceDTO> utterances = sttResponse.getUtterances();
                    if (utterances == null) {
                        utterances = Collections.emptyList();
                    }
                    return Flux.fromIterable(utterances)
                            .map(this::createAndSaveSpeechSegment);
                });
    }

    private SpeechSegment createAndSaveSpeechSegment(SttResponseDTO.UtteranceDTO utteranceDto) {
        // 각 발화를 SpeechSegment로 변환
        SpeechSegment segment = SpeechSegmentConverter.toSpeechSegment(
                utteranceDto,
                audioSessionBuffer.getMeeting(),
                audioSessionBuffer.getUtteranceStartTime()
        );

        log.info("Speaker {} said: {} (start at {})",
                segment.getSpeakerId(), segment.getText(), segment.getStartTime());

        // SpeechSegment를 모든 발화 내용을 저장해놓는 버퍼에 추가 및 DB에 저장
        audioSessionBuffer.putUtterance(segment);
        SpeechSegment savedSegment = speechSegmentRepository.save(segment);

        // 클라이언트에게 메시지 전송
        notificationService.sendUtteranceNotification(segment, utteranceDto.getSpeakerId());

        return savedSegment;
    }
}
