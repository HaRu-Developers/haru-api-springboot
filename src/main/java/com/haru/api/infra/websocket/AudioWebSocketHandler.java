package com.haru.api.infra.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.haru.api.domain.meeting.entity.Meeting;
import com.haru.api.domain.meeting.repository.MeetingRepository;
import com.haru.api.global.apiPayload.code.status.ErrorStatus;
import com.haru.api.global.apiPayload.exception.handler.MeetingHandler;
import com.haru.api.infra.api.client.ChatGPTClient;
import com.haru.api.infra.api.client.FastApiClient;
import com.haru.api.infra.api.client.ScoringApiClient;
import com.haru.api.infra.api.repository.AIQuestionRepository;
import com.haru.api.infra.api.repository.SpeechSegmentRepository;
import com.orctom.vad4j.VAD;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class AudioWebSocketHandler extends BinaryWebSocketHandler {

    private final Map<String, AudioSessionBuffer> sessionBuffers = new ConcurrentHashMap<>();
    private final Map<String, AudioProcessingQueue> sessionQueues = new ConcurrentHashMap<>();

    private final FastApiClient fastApiClient;
    private final ChatGPTClient chatGPTClient;
    private final ScoringApiClient scoringApiClient;

    private final MeetingRepository meetingRepository;
    private final SpeechSegmentRepository speechSegmentRepository;
    private final AIQuestionRepository aiQuestionRepository;

    private final ObjectMapper objectMapper;

    private final Pattern pathPattern = Pattern.compile("^/ws/audio/(\\w+)$");

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessionBuffers.put(session.getId(), new AudioSessionBuffer());

        String path = session.getUri().getPath();
        Matcher matcher = pathPattern.matcher(path);

        if (matcher.matches()) {
            Long meetingId = Long.parseLong(matcher.group(1));

            // meetingId를 활용하여 로직 처리
            System.out.println("Meeting ID: " + meetingId);

            Meeting foundMeeting = meetingRepository.findById(meetingId)
                            .orElseThrow(() -> new MeetingHandler(ErrorStatus.MEETING_NOT_FOUND));

            sessionBuffers.get(session.getId()).setMeeting(foundMeeting);
        } else {
            // 경로가 올바르지 않은 경우 처리
            session.close(CloseStatus.BAD_DATA.withReason("Invalid path"));
        }

        System.out.println("WebSocket 연결됨: " + session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionBuffers.remove(session.getId());
        sessionQueues.remove(session.getId());
        System.out.println("연결 종료: " + session.getId());
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {

        // websocket 으로 넘겨받은 640 bytes 음성 chunk (20ms)
        String sessionId = session.getId();
        byte[] audioChunk = message.getPayload().array();

        // session 의 sessionBuffer
        AudioSessionBuffer sessionBuffer = sessionBuffers.get(sessionId);
        if (sessionBuffer == null) return;

        // 버퍼에 chunk 추가
        sessionBuffer.appendFullBuffer(audioChunk);

        // vad 사용해서 해당 chunk가 음성인지 판단
        try (VAD vad = new VAD()) {
            boolean isSpeech = vad.isSpeech(audioChunk);

            boolean isTriggered = sessionBuffer.getIsTriggered();

            // 음성 녹음 전
            if (!isTriggered) {

                // chunk가 음성인 경우
                // 음성 버퍼에 데이터 저장
                if(isSpeech) {
                    sessionBuffer.appendCurrentUtteranceBuffer(audioChunk);
                    sessionBuffer.setNoVoiceCount(0);
                    sessionBuffer.setIsTriggered(true);
                    sessionBuffer.resetCurrentUtteranceBuffer();

                    // todo: 음성이 시작된 시간 기록 (완료)
                    sessionBuffer.setUtteranceStartTime(LocalDateTime.now());

                    log.info("isTriggered: {}", sessionBuffer.getIsTriggered());
                }

                // chunk가 음성이 아닌 경우
                // 침묵이므로 아무것도 안함

            } else { // 음성 녹음 중

                // chunk가 음성인 경우
                // 음성 버퍼에 데이터 저장
                if(isSpeech) {
                    sessionBuffer.appendCurrentUtteranceBuffer(audioChunk);
                    sessionBuffer.setNoVoiceCount(0);
                } else {
                    // chunk가 음성이 아닌 경우
                    // noVoiceCount 증가
                    int noVoiceCount = sessionBuffer.getNoVoiceCount();
                    sessionBuffer.setNoVoiceCount(noVoiceCount + 20);

                    // noVoiceCount 가 임계값에 도달한 경우, 음성의 끝이라고 판단
                    if (sessionBuffer.getNoVoiceCount() >= AudioSessionBuffer.NO_VOICE_COUNT_TARGET) {

                        // 세션별 큐가 없으면 생성
                        sessionQueues.computeIfAbsent(sessionId, id ->
                            new AudioProcessingQueue(
                                    fastApiClient::sendRawBytesToFastAPI,
                                    scoringApiClient::sendScoringRequstToFastAPI,
                                    chatGPTClient,
                                    session,
                                    sessionBuffer,
                                    speechSegmentRepository,
                                    aiQuestionRepository,
                                    objectMapper
                            )
                        );

                        // 큐에 넣기 (순서 보장)
                        sessionQueues.get(sessionId).enqueue(sessionBuffer.getCurrentUtteranceBuffer());
                        log.info("speech detected");

                        sessionBuffer.resetCurrentUtteranceBuffer();
                        sessionBuffer.setIsTriggered(false);
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
