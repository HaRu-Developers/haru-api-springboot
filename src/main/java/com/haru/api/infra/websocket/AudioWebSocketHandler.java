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
import com.haru.api.infra.mp3encoder.Mp3EncoderService;
import com.haru.api.infra.s3.AmazonS3Manager;
import com.orctom.vad4j.VAD;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.ByteArrayOutputStream;
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

    private final AmazonS3Manager s3Manager;

    private final Mp3EncoderService encoderService;

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

            // meeting의 회의 시작 시간 기록
            foundMeeting.initStartTime(LocalDateTime.now());

            sessionBuffers.get(session.getId()).setMeeting(foundMeeting);
        } else {
            // 경로가 올바르지 않은 경우 처리
            session.close(CloseStatus.BAD_DATA.withReason("Invalid path"));
        }
        System.out.println("WebSocket 연결됨: " + session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = session.getId();

        // 1. 해당 세션의 전체 오디어 버퍼 가져오기
        ByteArrayOutputStream audioBuffer = sessionBuffers.get(sessionId).getAllBytes();

        // 2. 버퍼에 데이터가 있는 지 확인
        if (audioBuffer != null && audioBuffer.size() > 0) {
            try {
                // 3. 원본 오디오 데이터를 byte[]로 변환
                byte[] rawAudioData = audioBuffer.toByteArray();
                log.info("세션 ID {}의 오디오 데이터 처리 시작. 원본 크기: {} bytes", sessionId, rawAudioData.length);

                // 4. 클라이언트 오디오 설정에 맞춰 MP3로 인코딩
                int channels = 1;
                int samplingRate = 16000; // 16kHz
                int bitRate = 128000;     // 128kbps
                byte[] mp3Data = encoderService.encodePcmToMp3(rawAudioData, channels, samplingRate, bitRate);
                log.info("MP3 인코딩 완료. 인코딩된 크기: {} bytes", mp3Data.length);

                // 5. S3에 저장할 고유한 키를 생성 및 저장 (확장자 .mp3 추가)
                String keyName = s3Manager.generateKeyName("meeting/recording") + ".mp3";

                // 6. S3에 인코딩된 MP3 파일을 업로드
                s3Manager.uploadFile(keyName, mp3Data, "audio/mpeg"); // MP3의 MIME 타입은 "audio/mpeg"
                log.info("S3 업로드 성공. Key: {}", keyName);

                // 7. meeting entity에 audio key name 저장
                Meeting currentMeeting = sessionBuffers.get(sessionId).getMeeting();
                currentMeeting.setAudioFileKey(keyName);
                meetingRepository.save(currentMeeting);

            } catch (Exception e) {
                log.error("세션 ID {}의 오디오 처리 및 S3 업로드 중 오류 발생", sessionId, e);
            }
        } else {
            log.warn("세션 ID {}에 처리할 오디오 데이터가 없습니다.", sessionId);
        }

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
