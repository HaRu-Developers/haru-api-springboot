package com.haru.api.infra.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.util.function.Function;

@Slf4j
public class AudioProcessingQueue {

    private final Sinks.Many<byte[]> sink;
    private final Flux<byte[]> flux;

    public AudioProcessingQueue(Function<byte[], Mono<String>> sttFunction, WebSocketSession session) {
        // 단일 소비자용 Sink 생성 (queue 기반)
        this.sink = Sinks.many().unicast().onBackpressureBuffer();
        this.flux = sink.asFlux();

        // 비동기 순차 처리 시작
        this.flux
                .concatMap(buffer -> sttFunction.apply(buffer))
                .subscribe(result -> {
                    log.info("result: {}", result);
//                    try {
//                        session.sendMessage(new TextMessage(result));
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
                });
    }

    public void enqueue(byte[] buffer) {
        Sinks.EmitResult result = sink.tryEmitNext(buffer);
        if (result.isFailure()) {
            // 실패 처리: 큐가 닫혔거나 오류 상태일 수 있음
            System.err.println("Failed to enqueue audio buffer: " + result);
        }
    }
}
