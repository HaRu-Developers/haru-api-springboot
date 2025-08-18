package com.haru.api.infra.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.haru.api.infra.api.dto.AIQuestionResponse;
import com.haru.api.infra.api.dto.SpeechSegmentResponseDTO;
import com.haru.api.infra.api.dto.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
public class TestWebSocketHandler extends TextWebSocketHandler {

    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final AtomicLong speechIdCounter = new AtomicLong(1);

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 클라이언트가 연결되면 세션 목록에 추가
        sessions.put(session.getId(), session);
        System.out.println("Test WS - 클라이언트 연결됨: " + session.getId());
    }

    // 2초마다 실행되는 스케줄링 메서드
    @Scheduled(fixedRate = 2000)
    public void sendPeriodicMessages() {
        if (sessions.isEmpty()) {
            return; // 연결된 클라이언트가 없으면 실행하지 않음
        }

        // 보낼 테스트 데이터 생성
        long currentSpeechId = speechIdCounter.getAndIncrement();

        WebSocketMessage<?> messageToSend;

        // 50% 확률로 다른 타입의 메시지 전송
        if (Math.random() > 0.5) {
            SpeechSegmentResponseDTO.SpeechSegmentResponse speechData = SpeechSegmentResponseDTO.SpeechSegmentResponse.builder()
                    .speechId(currentSpeechId)
                    .speakerId("SPK_01")
                    .text("이것은 2초마다 전송되는 테스트 STT 데이터입니다. ID: " + currentSpeechId)
                    .startTime(LocalDateTime.now())
                    .build();
            messageToSend = new WebSocketMessage<>("SPEECH_SEGMENT", speechData);
        } else {
            AIQuestionResponse questionData = new AIQuestionResponse();
            questionData.setSpeechId(currentSpeechId);
            questionData.setQuestions(List.of("이 내용의 핵심 질문은 무엇인가요?", "다음 예상 질문은 무엇일까요?"));
            messageToSend = new WebSocketMessage<>("AI_QUESTION", questionData);
        }

        // 모든 연결된 클라이언트에게 메시지 전송
        sessions.values().forEach(session -> {
            try {
                if (session.isOpen()) {
                    String jsonMessage = objectMapper.writeValueAsString(messageToSend);
                    session.sendMessage(new TextMessage(jsonMessage));
                }
            } catch (IOException e) {
                System.err.println("메시지 전송 중 오류 발생: " + e.getMessage());
            }
        });
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 클라이언트 연결이 끊어지면 세션 목록에서 제거
        sessions.remove(session.getId());
        System.out.println("Test WS - 클라이언트 연결 끊김: " + session.getId());
    }
}
