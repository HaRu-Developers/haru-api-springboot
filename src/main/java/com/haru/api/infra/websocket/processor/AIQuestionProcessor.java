package com.haru.api.infra.websocket.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.haru.api.infra.api.client.ChatGPTClient;
import com.haru.api.infra.api.dto.AIQuestionResponse;
import com.haru.api.infra.api.entity.AIQuestion;
import com.haru.api.infra.api.entity.SpeechSegment;
import com.haru.api.infra.api.repository.AIQuestionRepository;
import com.haru.api.infra.websocket.AudioSessionBuffer;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Slf4j
public class AIQuestionProcessor {
    private final ChatGPTClient chatGPTClient;
    private final AIQuestionRepository aiQuestionRepository;
    private final AudioSessionBuffer audioSessionBuffer;
    private final WebSocketNotificationService notificationService;
    private final ObjectMapper objectMapper;

    public AIQuestionProcessor(ChatGPTClient chatGPTClient,
                               AIQuestionRepository aiQuestionRepository,
                               AudioSessionBuffer audioSessionBuffer,
                               WebSocketNotificationService notificationService,
                               ObjectMapper objectMapper) {
        this.chatGPTClient = chatGPTClient;
        this.aiQuestionRepository = aiQuestionRepository;
        this.audioSessionBuffer = audioSessionBuffer;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    public Mono<Void> processAIQuestions(SpeechSegment segment) {
        return Mono.fromCallable(() -> generateAIQuestions(segment))
                .flatMap(aiResponse -> saveAndNotifyAIQuestions(segment, aiResponse))
                .subscribeOn(Schedulers.boundedElastic()); // 비동기 처리
    }

    private AIQuestionResponse generateAIQuestions(SpeechSegment segment) {
        try {
            String aiQuestionsJson = chatGPTClient.getAIQuestionsRaw(audioSessionBuffer.getAllUtterance());
            return objectMapper.readValue(aiQuestionsJson, AIQuestionResponse.class);
        } catch (Exception e) {
            log.error("Failed to generate AI questions for segment: {}", segment.getId(), e);
            throw new RuntimeException("AI question generation failed", e);
        }
    }

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
                    .speechSegment(segment)
                    .question(questionText)
                    .build();
            aiQuestionRepository.save(aiQuestion);
        });
    }
}
