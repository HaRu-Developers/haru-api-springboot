package com.haru.api.infra.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.haru.api.infra.api.client.ChatGPTClient;
import com.haru.api.infra.api.converter.SpeechSegmentConverter;
import com.haru.api.infra.api.dto.AIQuestionResponse;
import com.haru.api.infra.api.dto.SpeechSegmentResponseDTO;
import com.haru.api.infra.api.dto.SttResponseDTO;
import com.haru.api.infra.api.dto.WebSocketMessage;
import com.haru.api.infra.api.entity.SpeechSegment;
import com.haru.api.infra.api.repository.SpeechSegmentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@Slf4j
public class AudioProcessingQueue {

    private final Sinks.Many<byte[]> sink;
    private final Flux<byte[]> flux;

    public AudioProcessingQueue(Function<byte[], Mono<String>> sttFunction,
                                ChatGPTClient chatGPTClient,
                                WebSocketSession session,
                                AudioSessionBuffer audioSessionBuffer,
                                SpeechSegmentRepository speechSegmentRepository,
                                ObjectMapper objectMapper
    ) {
        // 단일 소비자용 Sink 생성 (queue 기반)
        this.sink = Sinks.many().unicast().onBackpressureBuffer();
        this.flux = sink.asFlux();

        // 비동기 순차 처리 시작
        this.flux
                .concatMap(buffer -> sttFunction.apply(buffer))
                .subscribe(result -> {

                    try {
                        // FastAPI로 받은 JSON을 직렬화를 통해 SttResponseDto로 변환
                        SttResponseDTO sttResponse = objectMapper.readValue(result, SttResponseDTO.class);

                        // SttResponseDTO에 들어있는 utterances를 순회하면서 처리
                        sttResponse.getUtterances().forEach((utteranceDto) -> {

                            // 각 발언을 SpeechSegment로 변환
                            SpeechSegment segment = SpeechSegmentConverter.toSpeechSegment(
                                    utteranceDto,
                                    audioSessionBuffer.getMeeting(),
                                    audioSessionBuffer.getUtteranceStartTime()
                            );

                            log.info("Speaker {} said: {} (start at {})", segment.getSpeakerId(), segment.getText(), segment.getStartTime());

                            // todo: 텍스트 db에 저장 (Redis, MySQL) (완료)
                            // todo: db 저장 형식 (텍스트 ID, 텍스트 내용, 회의 ID, 화자 구분 번호, 발언 시작 시간) (완료)
                            audioSessionBuffer.putUtterance(segment);
                            speechSegmentRepository.save(segment);

                            // todo: 클라이언트에게 텍스트 전달 (완료)
                            try {
                                WebSocketMessage<SpeechSegmentResponseDTO.SpeechSegmentResponse> utteranceMsg =
                                        WebSocketMessage.<SpeechSegmentResponseDTO.SpeechSegmentResponse>builder()
                                                .type("utterance")
                                                .data(SpeechSegmentConverter.toSpeechSegmentResponseDTO(segment))
                                                .build();

                                String json = objectMapper.writeValueAsString(utteranceMsg);
                                session.sendMessage(new TextMessage(json));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }

                            // todo: 유의미하다고 판단된 문장에 대해 chatGPT API를 사용해 질문 받아오기
                            if (isValidSpeech(segment)) {
                                CompletableFuture.runAsync(() -> {
                                    String aiQuestionsJson = chatGPTClient.getAIQuestionsRaw(audioSessionBuffer.getAllUtterance());
                                    try {
                                        // AIQuestionResponse로 직렬화하여 유효성 검증
                                        AIQuestionResponse aiResponse = objectMapper.readValue(aiQuestionsJson, AIQuestionResponse.class);

                                        WebSocketMessage<AIQuestionResponse> aiMsg = WebSocketMessage.<AIQuestionResponse>builder()
                                                .type("ai_questions")
                                                .data(aiResponse)
                                                .build();

                                        String json = objectMapper.writeValueAsString(aiMsg);
                                        session.sendMessage(new TextMessage(json));

                                    } catch (IOException e) {
                                        log.error("AI 질문 전송 실패", e);
                                    }
                                });
                            }

                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    public void enqueue(byte[] buffer) {
        Sinks.EmitResult result = sink.tryEmitNext(buffer);
        if (result.isFailure()) {
            // 실패 처리: 큐가 닫혔거나 오류 상태일 수 있음
            System.err.println("Failed to enqueue audio buffer: " + result);
        }
    }

    private boolean isValidSpeech(SpeechSegment speechSegment) {
        return false;
    }
}
