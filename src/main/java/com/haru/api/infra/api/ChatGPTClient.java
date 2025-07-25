package com.haru.api.infra.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haru.api.infra.api.dto.AIQuestionResponse;
import com.haru.api.infra.api.dto.OpenAIResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class ChatGPTClient {

    private final WebClient webClient;

    public ChatGPTClient(@Qualifier("chatGPTWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public String getAIQuestionsRaw(String userMessageContent) {
        List<Map<String, String>> messages = List.of(
                Map.of(
                        "role", "system",
                        "content", "너는 실시간 회의 도우미야. 회의 참가자들의 발화를 계속 듣고, 회의의 맥락을 파악해서 그에 도움이 될 수 있는 질문을 실시간으로 제안하는 역할이야. 회의 흐름을 방해하지 않으면서, 더 깊이 있는 논의를 유도할 수 있는 질문을 제안해줘. 반드시 JSON 형식으로 응답해. 형식은 {\"questions\": [\"질문1\", \"질문2\", \"질문3\"]} 이며, 질문은 최대 3개까지만 포함해야 하고, 꼭 3개일 필요는 없어."
                ),
                Map.of(
                        "role", "user",
                        "content", "지금까지 발화된 내용은 다음과 같아: " + userMessageContent
                )
        );

        return webClient.post()
                .bodyValue(Map.of(
                        "model", "gpt-4o",
                        "messages", messages,
                        "temperature", 0.7
                ))
                .retrieve()
                .bodyToMono(OpenAIResponse.class)
                .map(response -> {
                    String content = response.getChoices().get(0).getMessage().getContent();
                    System.out.println("GPT 응답 내용: " + content);
                    return content;
                })
                .block(); // 테스트용: 동기적으로 결과 받을 수 있도록 block()
    }

    // todo: [(text ID, text) , ...] 형식으로 파라미터 받도록 수정
    public Mono<AIQuestionResponse> getAIQuestions(String userMessageContent) {

        List<Map<String, String>> messages = List.of(
                Map.of(
                        "role", "system",
                        "content", "너는 실시간 회의 도우미야. 회의 참가자들의 발화를 계속 듣고, 회의의 맥락을 파악해서 그에 도움이 될 수 있는 질문을 실시간으로 제안하는 역할이야. 회의 흐름을 방해하지 않으면서, 더 깊이 있는 논의를 유도할 수 있는 질문을 제안해줘. 반드시 JSON 형식으로 응답해. 형식은 {\"questions\": [\"질문1\", \"질문2\", \"질문3\"]} 이며, 질문은 최대 3개까지만 포함해야 해.\""
                ),
                Map.of(
                        "role", "user",
                        "content", "지금까지 발화된 내용은 다음과 같아: " + userMessageContent
                )
        );

        return webClient.post()
                .bodyValue(Map.of(
                        "model", "gpt-4o",
                        "messages", messages,
                        "temperature", 0.7
                ))
                .retrieve()
                .bodyToMono(OpenAIResponse.class)
                .handle((response, sink) -> {
                    String content = response.getChoices().get(0).getMessage().getContent();
                    // todo: 응답 형식 {"text_id": 10, "questions": ["질문1", "질문2", "질문3"]}
                    try {
                        sink.next(new ObjectMapper().readValue(content, AIQuestionResponse.class)); // 최종 질문 DTO로 변환
                    } catch (JsonProcessingException e) {
                        sink.error(new RuntimeException(e));
                    }
                });
    }
}
