package com.haru.api.infra.websocket.processor;

import com.haru.api.infra.api.dto.ScoringRequestDTO;
import com.haru.api.infra.api.dto.ScoringResponseDTO;
import com.haru.api.infra.api.entity.SpeechSegment;
import com.haru.api.infra.websocket.AudioSessionBuffer;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Function;

@Slf4j
public class ScoringProcessor {
    private final Function<ScoringRequestDTO, Mono<ScoringResponseDTO>> scoringFunction;
    private final AudioSessionBuffer audioSessionBuffer;

    public ScoringProcessor(Function<ScoringRequestDTO, Mono<ScoringResponseDTO>> scoringFunction,
                            AudioSessionBuffer audioSessionBuffer) {
        this.scoringFunction = scoringFunction;
        this.audioSessionBuffer = audioSessionBuffer;
    }

    // 각 speech segment에 대해서 FastAPI의 scoring API 호출
    public Mono<ScoringResponseDTO> processScoring(SpeechSegment segment) {
        ScoringRequestDTO scoringRequest = createScoringRequest(segment);
        return scoringFunction.apply(scoringRequest)
                .doOnError(error -> log.error("scoring API 호출 실패", error))
                .onErrorResume(error -> Mono.empty())
                .doOnNext(response -> {
                    if (response.getIsQuestionNeeded()) {
                        log.info("Question is needed for segment: {}", segment.getId());
                    } else {
                        log.info("Question is not needed for segment: {}", segment.getId());
                    }
                });
    }

    private ScoringRequestDTO createScoringRequest(SpeechSegment segment) {
        List<String> allUtterances = audioSessionBuffer.getAllUtterancesAsList();

        return ScoringRequestDTO.builder()
                .speechId(segment.getId())
                .utterance(segment.getText())
                .hasAgenda(audioSessionBuffer.getAgenda() != null)
                .agendaText(audioSessionBuffer.getAgenda())
                .recentUtterances(allUtterances)
                .build();
    }
}
