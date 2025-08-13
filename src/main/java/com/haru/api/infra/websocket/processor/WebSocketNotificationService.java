package com.haru.api.infra.websocket.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.haru.api.infra.api.converter.SpeechSegmentConverter;
import com.haru.api.infra.api.dto.AIQuestionResponse;
import com.haru.api.infra.api.dto.SpeechSegmentResponseDTO;
import com.haru.api.infra.api.dto.WebSocketMessage;
import com.haru.api.infra.api.entity.SpeechSegment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

@Slf4j
public class WebSocketNotificationService {
    private final WebSocketSession session;
    private final ObjectMapper objectMapper;

    public WebSocketNotificationService(WebSocketSession session, ObjectMapper objectMapper) {
        this.session = session;
        this.objectMapper = objectMapper;
    }

    public void sendUtteranceNotification(SpeechSegment segment, String speakerId) {
        try {
            WebSocketMessage<SpeechSegmentResponseDTO.SpeechSegmentResponse> message =
                    WebSocketMessage.<SpeechSegmentResponseDTO.SpeechSegmentResponse>builder()
                            .type("utterance")
                            .data(SpeechSegmentConverter.toSpeechSegmentResponseDTO(segment, speakerId))
                            .build();

            sendMessage(message);
        } catch (Exception e) {
            log.error("Failed to send utterance notification", e);
        }
    }

    public void sendAIQuestionsNotification(AIQuestionResponse aiResponse) {
        try {
            WebSocketMessage<AIQuestionResponse> message =
                    WebSocketMessage.<AIQuestionResponse>builder()
                            .type("ai_questions")
                            .data(aiResponse)
                            .build();

            sendMessage(message);
        } catch (Exception e) {
            log.error("Failed to send AI questions notification", e);
        }
    }

    private void sendMessage(Object message) throws IOException {
        String json = objectMapper.writeValueAsString(message);
        session.sendMessage(new TextMessage(json));
    }
}
