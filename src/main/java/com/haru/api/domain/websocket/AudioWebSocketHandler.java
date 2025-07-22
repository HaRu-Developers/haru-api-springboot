package com.haru.api.domain.websocket;

import com.orctom.vad4j.VAD;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class AudioWebSocketHandler extends BinaryWebSocketHandler {

    private final Map<String, AudioSessionBuffer> sessionBuffers = new ConcurrentHashMap<>();

    private final Map<String, AudioProcessingQueue> sessionQueues = new ConcurrentHashMap<>();

    private final FastApiClient fastApiClient = new FastApiClient();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessionBuffers.put(session.getId(), new AudioSessionBuffer());
        System.out.println("WebSocket 연결됨: " + session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionBuffers.remove(session.getId());
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

                        // stt api 호출
                        //String result = sttService.transcribe(sessionBuffer.getCurrentUtteranceBuffer());
                        //String result = fastApiClient.sendRawBytesToFastAPI(sessionBuffer.getCurrentUtteranceBuffer());

                        // 세션별 큐가 없으면 생성
                        sessionQueues.computeIfAbsent(sessionId, id ->
                            new AudioProcessingQueue(
                                    fastApiClient::sendRawBytesToFastAPI,
                                    session
                            )
                        );

                        // 큐에 넣기 (순서 보장)
                        sessionQueues.get(sessionId).enqueue(sessionBuffer.getCurrentUtteranceBuffer());
                        log.info("speech detected");

//                        fastApiClient.sendRawBytesToFastAPI(sessionBuffer.getCurrentUtteranceBuffer())
//                                        .subscribe(result -> {
//                                            log.info("stt api 응답: {}", result);
//                                        });

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
