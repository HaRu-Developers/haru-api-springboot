package com.haru.api.infra.websocket.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.haru.api.infra.api.client.ChatGPTClient;
import com.haru.api.infra.api.dto.AIQuestionResponse;
import com.haru.api.infra.api.entity.AIQuestion;
import com.haru.api.infra.api.entity.SpeechSegment;
import com.haru.api.infra.api.repository.AIQuestionRepository;
import com.haru.api.infra.api.repository.SpeechSegmentRepository;
import com.haru.api.infra.websocket.AudioSessionBuffer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Slf4j
public class AIQuestionProcessor {
    private final ChatGPTClient chatGPTClient;
    private final AIQuestionRepository aiQuestionRepository;
    private final SpeechSegmentRepository speechSegmentRepository;
    private final AudioSessionBuffer audioSessionBuffer;
    private final WebSocketNotificationService notificationService;
    private final ObjectMapper objectMapper;

    public AIQuestionProcessor(ChatGPTClient chatGPTClient,
                               AIQuestionRepository aiQuestionRepository,
                               AudioSessionBuffer audioSessionBuffer,
                               WebSocketNotificationService notificationService,
                               SpeechSegmentRepository speechSegmentRepository,
                               ObjectMapper objectMapper) {
        this.chatGPTClient = chatGPTClient;
        this.aiQuestionRepository = aiQuestionRepository;
        this.audioSessionBuffer = audioSessionBuffer;
        this.notificationService = notificationService;
        this.speechSegmentRepository = speechSegmentRepository;
        this.objectMapper = objectMapper;
    }

    // 질문이 필요하다고 판단된 segment에 대하여 처리 과정
    public Mono<Void> processAIQuestions(SpeechSegment segment) {
        return Mono.fromCallable(() -> generateAIQuestions(segment))
                .flatMap(aiResponse -> saveAndNotifyAIQuestions(segment, aiResponse))
                .subscribeOn(Schedulers.boundedElastic()); // 비동기 처리
    }

    // 질문 생성하여 chatGPT에게서 json 형식으로 결과를 받아오고, 이를 AIQuestionResponse로 변환
    private AIQuestionResponse generateAIQuestions(SpeechSegment segment) {
        try {
            String aiQuestionsJson = chatGPTClient.getAIQuestionsRaw(audioSessionBuffer.getAllUtterance());
            return objectMapper.readValue(aiQuestionsJson, AIQuestionResponse.class);
        } catch (Exception e) {
            log.error("Failed to generate AI questions for segment: {}", segment.getId(), e);
            throw new RuntimeException("AI question generation failed", e);
        }
    }

    // AIQuestionResponse를 토대로 AI 질문을 저장하고, 클라이언트에게 보냄
    private Mono<Void> saveAndNotifyAIQuestions(SpeechSegment segment, AIQuestionResponse aiResponse) {
        return Mono.fromRunnable(() -> {
            if (aiResponse.getQuestions() != null) {
                saveAIQuestions(segment, aiResponse.getQuestions());
                aiResponse.setSpeechId(segment.getId());
                notificationService.sendAIQuestionsNotification(aiResponse);
            }
        });
    }


    private void saveAIQuestions(SpeechSegment segment, List<String> questions) {
        questions.forEach(questionText -> {
            AIQuestion aiQuestion = AIQuestion.builder()
                    .question(questionText)
                    .build();
            segment.addAIQuestion(aiQuestion);
        });
        speechSegmentRepository.save(segment);
    }
}
