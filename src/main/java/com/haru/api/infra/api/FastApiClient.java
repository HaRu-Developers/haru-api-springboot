package com.haru.api.infra.api;

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

    /**
     *
     * @param audioBytes : 종단점이라고 파악된 문장의 byte[] 데이터
     * @return : stt api에 의한 텍스트
     */
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
