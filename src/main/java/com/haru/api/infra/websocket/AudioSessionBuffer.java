package com.haru.api.infra.websocket;

import com.haru.api.domain.meeting.entity.Meeting;
import com.haru.api.infra.api.entity.SpeechSegment;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class AudioSessionBuffer {

    // 전체 음성 데이터의 byte[]를 담아두기 위한 배열
    private final ByteArrayOutputStream fullBuffer = new ByteArrayOutputStream();

    // 종단점이 파악되기 전의 음성 데이터의 byte[]를 담아두기 위한 배열
    // 종단점이 파악되면 데이터를 fastapi로 보내고, 해당 버퍼를 초기화함
    private final ByteArrayOutputStream currentUtteranceBuffer = new ByteArrayOutputStream();

    // 회의를 하면서 stt로 변환된 텍스트를 담아두기 위한 queue
    private final Queue<SpeechSegment> currentUtteranceQueue = new LinkedList<>();

    // 상태
    private boolean isTriggered = false;

    private int noVoiceCount = 0;

    public static final int NO_VOICE_COUNT_TARGET = 200;

    private LocalDateTime utterance_start_time;

    private Meeting meeting;

    // 메서드
    public synchronized void appendFullBuffer(byte[] chunk) {
        fullBuffer.write(chunk, 0, chunk.length);
    }

    public synchronized void appendCurrentUtteranceBuffer(byte[] chunk) {
        currentUtteranceBuffer.write(chunk, 0, chunk.length);
    }

    public synchronized byte[] getAllBytes() {
        return fullBuffer.toByteArray();
    }

    public synchronized byte[] getCurrentUtteranceBuffer() {
        return currentUtteranceBuffer.toByteArray();
    }

    public synchronized void resetCurrentUtteranceBuffer() {
        currentUtteranceBuffer.reset();
    }

    public synchronized boolean getIsTriggered() {
        return isTriggered;
    }

    public synchronized void setIsTriggered(boolean isTriggered) {
        this.isTriggered = isTriggered;
    }

    public synchronized int getNoVoiceCount() {
        return noVoiceCount;
    }

    public synchronized void setNoVoiceCount(int noVoiceCount) {
        this.noVoiceCount = noVoiceCount;
    }

    public synchronized void putUtterance(SpeechSegment speechSegment) {
        currentUtteranceQueue.offer(speechSegment);
    }

    public synchronized Queue<SpeechSegment> getCurrentUtteranceQueue() {
        return currentUtteranceQueue;
    }

    public synchronized String getAllUtterance() {
        if (currentUtteranceQueue.isEmpty()) {
            return "No utterances recorded yet.";
        }

        List<SpeechSegment> sortedSegments = currentUtteranceQueue.stream()
                .sorted(Comparator.comparing(SpeechSegment::getStartTime))
                .toList();

        StringBuilder sb = new StringBuilder();

        for (SpeechSegment segment : sortedSegments) {
            sb.append(segment.toString()).append("\n"); // SpeechSegment의 toString() 사용
        }

        return sb.toString();
    }

    public synchronized List<String> getAllUtterancesAsList() {
        // 큐가 비어있으면 빈 리스트를 반환
        if (currentUtteranceQueue.isEmpty()) {
            return Collections.emptyList();
        }

        // 큐의 각 SpeechSegment에서 utterance만 추출하여 새 리스트에 저장
        return currentUtteranceQueue.stream()
                .map(SpeechSegment::getText)
                .collect(Collectors.toList());
    }

    public synchronized void setUtteranceStartTime(LocalDateTime utterance_start_time) {
        this.utterance_start_time = utterance_start_time;
    }

    public synchronized LocalDateTime getUtteranceStartTime() {
        return utterance_start_time;
    }

    public synchronized void setMeeting(Meeting meeting) {
        this.meeting = meeting;
    }

    public synchronized Meeting getMeeting() {
        return meeting;
    }

    public synchronized String getAgenda() {
        return meeting.getAgendaResult();
    }
}
