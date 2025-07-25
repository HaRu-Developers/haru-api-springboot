package com.haru.api.infra.websocket;

import java.io.ByteArrayOutputStream;
import java.util.LinkedList;
import java.util.Queue;

public class AudioSessionBuffer {

    // 전체 음성 데이터의 byte[]를 담아두기 위한 배열
    private final ByteArrayOutputStream fullBuffer = new ByteArrayOutputStream();

    // 종단점이 파악되기 전의 음성 데이터의 byte[]를 담아두기 위한 배열
    // 종단점이 파악되면 데이터를 fastapi로 보내고, 해당 버퍼를 초기화함
    private final ByteArrayOutputStream currentUtteranceBuffer = new ByteArrayOutputStream();

    // 회의를 하면서 stt로 변환된 텍스트를 담아두기 위한 queue
    private final Queue<String> currentUtteranceQueue = new LinkedList<>();

    // 상태
    private boolean isTriggered = false;

    private int noVoiceCount = 0;

    public static final int NO_VOICE_COUNT_TARGET = 300;

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

    public synchronized void putUtterance(String utterance) {
        currentUtteranceQueue.offer(utterance);
    }

    public synchronized String getAllUtterance() {
        return String.join("\n", currentUtteranceQueue);
    }

}
