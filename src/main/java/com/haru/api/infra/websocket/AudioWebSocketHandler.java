package com.haru.api.infra.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.haru.api.domain.meeting.entity.Meeting;
import com.haru.api.domain.meeting.repository.MeetingRepository;
import com.haru.api.domain.meeting.service.MeetingCommandService;
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
    private final WebSocketSessionRegistry webSocketSessionRegistry;

    private final FastApiClient fastApiClient;
    private final ChatGPTClient chatGPTClient;
    private final ScoringApiClient scoringApiClient;

    private final MeetingRepository meetingRepository;
    private final SpeechSegmentRepository speechSegmentRepository;
    private final AIQuestionRepository aiQuestionRepository;

    private final ObjectMapper objectMapper;

    private final MeetingCommandService meetingCommandService;

    private final Pattern pathPattern = Pattern.compile("^/ws/audio/(\\w+)$");

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessionBuffers.put(session.getId(), new AudioSessionBuffer());

        String path = session.getUri().getPath();
        Matcher matcher = pathPattern.matcher(path);

        if (matcher.matches()) {
            Long meetingId = Long.parseLong(matcher.group(1));

            // WebSocketSessionRegistry에 meetingId를 key로 session 추가
            webSocketSessionRegistry.addSession(meetingId, session);

            // meetingId를 활용하여 로직 처리
            log.info("Meeting ID: {}", meetingId);

            Meeting foundMeeting = meetingRepository.findById(meetingId)
                            .orElseThrow(() -> new MeetingHandler(ErrorStatus.MEETING_NOT_FOUND));

            // meeting의 회의 시작 시간 기록
            foundMeeting.initStartTime(LocalDateTime.now());

            sessionBuffers.get(session.getId()).setMeeting(foundMeeting);
        } else {
            // 경로가 올바르지 않은 경우 처리
            session.close(CloseStatus.BAD_DATA.withReason("Invalid path"));
        }
        log.info("WebSocket 연결됨: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = session.getId();

        // 회의 종료 후, 회의 음성 파일 s3 업로드, AI 회의록 생성
        meetingCommandService.processAfterMeeting(sessionBuffers.get(sessionId));

        sessionBuffers.remove(sessionId);
        sessionQueues.remove(sessionId);
        log.info("연결 종료: {}", sessionId);
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

                    // 발화가 시작된 시간 버퍼에 저장
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

                        // 세션별 발화를 처리하기 위한 큐가 없으면 생성
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

                        // 현재 발화를 처리했으므로, 발화를 임시로 저장해놓는 버퍼 초기화
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
