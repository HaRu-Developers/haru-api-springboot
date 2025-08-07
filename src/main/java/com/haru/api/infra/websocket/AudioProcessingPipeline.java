package com.haru.api.infra.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.haru.api.infra.api.client.ChatGPTClient;
import com.haru.api.infra.api.dto.ScoringRequestDTO;
import com.haru.api.infra.api.dto.ScoringResponseDTO;
import com.haru.api.infra.api.entity.SpeechSegment;
import com.haru.api.infra.api.repository.AIQuestionRepository;
import com.haru.api.infra.api.repository.SpeechSegmentRepository;
import com.haru.api.infra.websocket.processor.AIQuestionProcessor;
import com.haru.api.infra.websocket.processor.ScoringProcessor;
import com.haru.api.infra.websocket.processor.SpeechSegmentProcessor;
import com.haru.api.infra.websocket.processor.WebSocketNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Function;

@Slf4j
public class AudioProcessingPipeline {
    private final Function<byte[], Mono<String>> sttFunction;
    private final SpeechSegmentProcessor speechSegmentProcessor;
    private final ScoringProcessor scoringProcessor;
    private final AIQuestionProcessor aiQuestionProcessor;
    private final WebSocketNotificationService notificationService;

    public AudioProcessingPipeline(
            Function<byte[], Mono<String>> sttFunction,
            Function<ScoringRequestDTO, Mono<ScoringResponseDTO>> scoringFunction,
            ChatGPTClient chatGPTClient,
            WebSocketSession session,
            AudioSessionBuffer audioSessionBuffer,
            SpeechSegmentRepository speechSegmentRepository,
            AIQuestionRepository aiQuestionRepository,
            ObjectMapper objectMapper
    ) {
        this.sttFunction = sttFunction;
        this.notificationService = new WebSocketNotificationService(session, objectMapper);
        this.speechSegmentProcessor = new SpeechSegmentProcessor(
                audioSessionBuffer, speechSegmentRepository, notificationService, objectMapper
        );
        this.scoringProcessor = new ScoringProcessor(scoringFunction, audioSessionBuffer);
        this.aiQuestionProcessor = new AIQuestionProcessor(
                chatGPTClient, aiQuestionRepository, audioSessionBuffer, notificationService, objectMapper
        );
    }

    // 버퍼에 음성 바이트 스트림이 들어오면 FastAPI의 STT API를 호출하여 결과를 받아옴
    public Mono<Void> processAudioBuffer(byte[] audioBuffer) {
        return sttFunction.apply(audioBuffer)
                .flatMap(this::processSttResponse)
                .onErrorResume(error -> {
                    log.error("Error in audio processing pipeline", error);
                    return Mono.empty();
                });
    }

    // STT API를 호출하여 받아온 결과를 SpeechSegmentProcessor를 사용하여
    // 각 speech를 생성하고 저장
    private Mono<Void> processSttResponse(String sttResult) {
        return speechSegmentProcessor.processSttResult(sttResult)
                .flatMapMany(this::processEachSpeechSegment)
                .then();
    }

    // 각 speech에 대해서 처리
    private Flux<Void> processEachSpeechSegment(List<SpeechSegment> segments) {
        return Flux.fromIterable(segments)
                .flatMap(this::processSingleSegment);
    }

    // 각 speech에 대해서 processScoring을 사용하여 FastAPI의 scoring API 호출
    private Mono<Void> processSingleSegment(SpeechSegment segment) {
        return scoringProcessor.processScoring(segment)
                .flatMap(scoringResponse -> {
                    if (scoringResponse.getIsQuestionNeeded()) {
                        return aiQuestionProcessor.processAIQuestions(segment);
                    }
                    return Mono.empty();
                });
    }
}
