package com.haru.api.infra.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class WebSocketSessionRegistry {

    private final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // 세션 연결 시 저장소에 추가
    public void addSession(Long key, WebSocketSession session) {
        sessions.put(key, session);
        System.out.println("[SessionRegistry] 세션 추가: " + key);
    }

    // meetingId로 세션 조회
    public WebSocketSession getSession(Long key) {
        return sessions.get(key);
    }

    // 세션 연결 종료 시 저장소에서 제거
    public void removeSession(Long key) {
        try {
            sessions.remove(key);
            log.info("[SessionRegistry] 세션 제거: {}", key);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
