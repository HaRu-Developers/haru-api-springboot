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

    public AudioProcessingQueue(Function<byte[], Mono<String>> sttFunction, WebSocketSession session, AudioSessionBuffer audioSessionBuffer) {
        // 단일 소비자용 Sink 생성 (queue 기반)
        this.sink = Sinks.many().unicast().onBackpressureBuffer();
        this.flux = sink.asFlux();

        // 비동기 순차 처리 시작
        this.flux
                .concatMap(buffer -> sttFunction.apply(buffer))
                .subscribe(result -> {

                    // todo: 텍스트 db에 저장 (Redis, MySQL)
                    // 현재는 메모리에 저장
                    audioSessionBuffer.putUtterance(result);
                    log.info("utterance queue: {}", audioSessionBuffer.getAllUtterance());

                    // todo: db 저장 형식 (텍스트 ID, 텍스트 내용, 회의 ID, 화자 구분 번호, 발언 시작 시간)

                    // todo: 클라이언트에게 텍스트 전달

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
