package com.haru.api.infra.api.client;

import com.haru.api.infra.api.dto.ScoringRequestDTO;
import com.haru.api.infra.api.dto.ScoringResponseDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class ScoringApiClient {

    private final WebClient webClient;

    public ScoringApiClient(@Qualifier("scoreApiWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<ScoringResponseDTO> sendScoringRequstToFastAPI(ScoringRequestDTO scoringRequestDTO) {
        return webClient.post()
                .bodyValue(scoringRequestDTO)
                .retrieve()
                .bodyToMono(ScoringResponseDTO.class);
    }
}
