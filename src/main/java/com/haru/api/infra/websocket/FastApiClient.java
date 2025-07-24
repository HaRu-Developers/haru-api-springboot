package com.haru.api.infra.websocket;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class FastApiClient {

    private final WebClient webClient;

    public FastApiClient(@Qualifier("fastApiWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<String> sendRawBytesToFastAPI(byte[] audioBytes) {
        return webClient.post()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .bodyValue(audioBytes)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(e -> {
                    e.printStackTrace();
                    return Mono.just("{\"error\": \"Failed to send\"}");
                });
    }

}
