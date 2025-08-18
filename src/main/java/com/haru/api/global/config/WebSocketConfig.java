package com.haru.api.global.config;

import com.haru.api.infra.websocket.AudioWebSocketHandler;
import com.haru.api.infra.websocket.TestWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final AudioWebSocketHandler audioWebSocketHandler;
    private final TestWebSocketHandler testWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(audioWebSocketHandler, "/ws/audio/{meetingId}")
                .setAllowedOrigins("*");  // Cross-Origin 허용

        registry.addHandler(testWebSocketHandler, "/ws/test")
                .setAllowedOrigins("*");
    }
}
