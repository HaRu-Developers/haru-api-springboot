package com.haru.api.domain.websocket;

import java.io.ByteArrayOutputStream;
import java.util.LinkedList;
import java.util.Queue;

public class AudioSessionBuffer {

    private final ByteArrayOutputStream fullBuffer = new ByteArrayOutputStream();

    private final ByteArrayOutputStream currentUtteranceBuffer = new ByteArrayOutputStream();

    private final Queue<byte[]> audioQueue = new LinkedList<>();

    // 상태
    private boolean isTriggered = false;

    private int noVoiceCount = 0;

    public static final int NO_VOICE_COUNT_TARGET = 300;

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

}
