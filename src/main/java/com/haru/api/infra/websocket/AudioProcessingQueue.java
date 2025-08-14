package com.haru.api.infra.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.haru.api.infra.api.client.ChatGPTClient;
import com.haru.api.infra.api.dto.*;
import com.haru.api.infra.api.repository.AIQuestionRepository;
import com.haru.api.infra.api.repository.SpeechSegmentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.function.Function;

@Slf4j
public class AudioProcessingQueue {

    private final Sinks.Many<byte[]> sink;
    private final Flux<byte[]> flux;
    private final AudioProcessingPipeline pipeline;

    public AudioProcessingQueue(Function<byte[], Mono<String>> sttFunction,
                                Function<ScoringRequestDTO, Mono<ScoringResponseDTO>> scoringFunction,
                                ChatGPTClient chatGPTClient,
                                WebSocketSession session,
                                AudioSessionBuffer audioSessionBuffer,
                                SpeechSegmentRepository speechSegmentRepository,
                                AIQuestionRepository aiQuestionRepository,
                                ObjectMapper objectMapper
    ) {
        // 단일 소비자용 Sink 생성 (queue 기반)
        this.sink = Sinks.many().unicast().onBackpressureBuffer(); // 데이터 발행
        this.flux = sink.asFlux(); // sink가 데이터를 flux 스트림으로 흘려보냄
        this.pipeline = new AudioProcessingPipeline(
                sttFunction, scoringFunction, chatGPTClient, session,
                audioSessionBuffer, speechSegmentRepository, aiQuestionRepository, objectMapper
        );

        initializeProcessingPipeline(); // 데이터가 들어오면 해당 메서드를 자동으로 실행
    }

    private void initializeProcessingPipeline() {
        this.flux
                // flatMapSequential을 통해서 1번 음성데이터가 stt 처리되는 동안, 2번 음성데이터가 들어오면
                // 1번 음성데이터가 stt 완료될때까지 기다리는게 아니라 2번 음성데이터에 대해서 바로 stt 처리되도록 구현
                // 1번 2번에 대한 결과 순서는 보장
                .flatMapSequential(pipeline::processAudioBuffer)
                .subscribe( // flux를 구독하여, enqueue 될 때 마다 해당 파이프라인 실행
                        result -> log.info("Audio processing completed successfully"),
                        error -> log.error("Audio processing failed", error)
                );
    }

    public void enqueue(byte[] buffer) {
        Sinks.EmitResult result = sink.tryEmitNext(buffer);
        if (result.isFailure()) {
            // 실패 처리: 큐가 닫혔거나 오류 상태일 수 있음
            System.err.println("Failed to enqueue audio buffer: " + result);
        }
    }
}
