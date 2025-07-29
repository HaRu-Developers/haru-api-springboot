package com.haru.api.infra.api.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haru.api.infra.api.dto.AIQuestionResponse;
import com.haru.api.infra.api.dto.OpenAIResponse;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
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

    public Mono<String> summarizePdf(MultipartFile pdfFile) {
        try (PDDocument document = PDDocument.load(pdfFile.getInputStream())) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            int pageCount = document.getNumberOfPages(); // PDF의 전체 페이지 수 가져오기

            // 1. 요청 본문의 content 부분을 담을 리스트 생성
            List<Map<String, Object>> contentParts = new ArrayList<>();

            // 1-1. 첫 번째 파트로 텍스트 프롬프트 추가
            contentParts.add(Map.of(
                    "type", "text",
                    "text", "이 이미지들은 하나의 연속된 문서입니다. 전체 내용을 종합해서 전체 내용을 종합해서 **반드시 255자 이내의 짧은 문단으로** 한국어로 요약해줘."
            ));

            // 2. 모든 페이지를 순회하며 이미지로 변환하고 리스트에 추가
            for (int i = 0; i < pageCount; i++) {
                BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI(i, 300); // i번째 페이지 렌더링

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, "png", baos);
                byte[] imageBytes = baos.toByteArray();
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);

                // 2-1. 이미지 파트를 리스트에 추가
                contentParts.add(Map.of(
                        "type", "image_url",
                        "image_url", Map.of("url", "data:image/png;base64," + base64Image)
                ));
            }

            // 3. 최종 요청 본문 구성
            List<Map<String, Object>> messages = List.of(
                    Map.of(
                            "role", "system",
                            "content", "너는 이미지에 있는 텍스트를 정확하게 읽고 핵심 내용을 간결하게 요약하는 전문 AI야."
                    ),
                    Map.of(
                            "role", "user",
                            "content", contentParts // 텍스트 프롬프트와 모든 이미지가 담긴 리스트 사용
                    )
            );

            Map<String, Object> requestBody = Map.of(
                    "model", "gpt-4o",
                    "messages", messages,
                    "max_tokens", 300
            );

            // 4. API 호출
            return webClient.post()
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(OpenAIResponse.class)
                    .map(response -> response.getChoices().get(0).getMessage().getContent());

        } catch (IOException e) {
            return Mono.error(new RuntimeException("파일을 이미지로 변환 중 오류가 발생했습니다.", e));
        }
    }
}
