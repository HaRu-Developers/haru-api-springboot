package com.haru.api.infra.api.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haru.api.infra.api.dto.SurveyReportResponse;
import com.haru.api.infra.api.dto.AIQuestionResponse;
import com.haru.api.infra.api.dto.OpenAIResponse;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
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
                        "content", "너는 실시간 회의 도우미야. 회의 참가자들의 발화를 계속 듣고, 회의의 맥락을 파악해서 그에 도움이 될 수 있는 질문을 실시간으로 제안하는 역할이야. 회의 흐름을 방해하지 않으면서, 더 깊이 있는 논의를 유도할 수 있는 질문을 제안해줘. 반드시 JSON 형식으로만 응답해야 하며, 절대로 백틱(`)이나 코드 블록을 포함하지 마. 형식은 {\"questions\": [\"질문1\", \"질문2\", \"질문3\"]} 이며, 질문은 최대 3개까지만 포함해야 하고, 꼭 3개일 필요는 없어."
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


    public String getMoodTrackerReportRaw(String userMessageContent) {
        StringBuilder sb = new StringBuilder();
        sb.append("너는 팀 심리 및 조직 문화 분석가야. 아래의 설문 응답을 통해 전체 설문을 종합한 마크다운 형식의 분석 리포트를 작성하고, 설문 질문 별로 개선 제안을 각 1개씩 제시해줘. 아래의 형식에서 ###이 붙은 제목은 고정이고, 내용은 너가 채워주면 돼.\n\n");

        sb.append("💡 최종 리포트 형식은 무조건 다음과 같은 마크다운 형식이어야만 합니다:\n");
        sb.append("### 1. 팀 분위기 트래커 리포트\n");
        sb.append("   - 대상과 목적, 분석 방식 등을 간단히 정리\n");
        sb.append("### 2. 주요 인사이트 요약 (HaRu AI가 뽑은 핵심 요약)\n");
        sb.append("   - 사용자의 응답 중 반복되거나 주목할 만한 인사이트를 요약\n");
        sb.append("   - 전체 응답자의 몇 %가 어떤 패턴을 보였는지도 서술\n");
        sb.append("   - 서술시에 질문 숫자 id가 아닌 질문 내용 텍스트 기반으로 설명\n");
        sb.append("### 3. 자유 응답 기반 주요 키워드 정리\n");
        sb.append("   - 예: 잦힌 (37건), 말은 덜 불분명 (29건) 등\n\n");

        sb.append("응답은 반드시 다음 JSON 형식으로 해줘. 형식만 따르고, 값은 생성한 값을 넣어줘야해. 질문에 대한 제안은 입력받은 질문 Id와 해당 질문에 매칭되는 제안 내용을 넣어줘. : \n");
        sb.append("{\n");
        sb.append("  \"report\": \"전체 리포트 마크다운 텍스트\",\n");
        sb.append("  \"suggestionsByQuestionId\": {\n");
        sb.append("    \"1\": \"질문 1에 대한 제안 내용\",\n");
        sb.append("    \"2\": \"질문 2에 대한 제안 내용\"\n");
        sb.append("  }\n");
        sb.append("}");

        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", sb.toString()),
                Map.of("role", "user", "content", userMessageContent)
        );

        log.debug("[GPT 요청 messages - RAW용] \n{}", messages);

        return webClient.post()
                .bodyValue(Map.of(
                        "model", "gpt-4o",
                        "messages", messages,
                        "temperature", 0.5
                ))
                .retrieve()
                .bodyToMono(OpenAIResponse.class)
                .map(response -> {
                    String content = response.getChoices().get(0).getMessage().getContent();
                    log.debug("[GPT 응답 메시지 content - RAW] \n{}", content);
                    return content;
                })
                .block(); // 개발 중 테스트 편의를 위해 동기 블록 처리
    }

    public Mono<SurveyReportResponse> getMoodTrackerReport(String userMessageContent) {
        StringBuilder sb = new StringBuilder();
        sb.append("너는 팀 심리 및 조직 문화 분석가야. 아래의 설문 응답을 통해 전체 설문을 종합한 마크다운 형식의 분석 리포트를 작성하고, 설문 질문 별로 개선 제안을 각 1개씩 제시해줘. 아래의 형식에서 ###이 붙은 제목은 고정이고, 내용은 너가 채워주면 돼.\n\n");

        sb.append("💡 최종 리포트 형식은 무조건 다음과 같은 마크다운 형식이어야만 합니다:\n");
        sb.append("### 1. 팀 분위기 트래커 리포트\n");
        sb.append("   - 대상과 목적, 분석 방식 등을 간단히 정리\n");
        sb.append("### 2. 주요 인사이트 요약 (HaRu AI가 뽑은 핵심 요약)\n");
        sb.append("   - 사용자의 응답 중 반복되거나 주목할 만한 인사이트를 요약\n");
        sb.append("   - 전체 응답자의 몇 %가 어떤 패턴을 보였는지도 서술\n");
        sb.append("   - 서술시에 질문 숫자 id가 아닌 질문 내용 텍스트 기반으로 설명\n");
        sb.append("### 3. 자유 응답 기반 주요 키워드 정리\n");
        sb.append("   - 예: 잦힌 (37건), 말은 덜 불분명 (29건) 등\n\n");

        sb.append("응답은 반드시 다음 JSON 형식으로 해줘. 벡틱(`)과 json 표시는 제외하고 본문만 출력해줘야해. 형식만 따르고, 값은 생성한 값을 넣어줘야해. 질문에 대한 제안은 입력받은 질문 Id와 해당 질문에 매칭되는 제안 내용을 넣어줘. : \n");
        sb.append("{\n");
        sb.append("  \"report\": \"전체 리포트 마크다운 텍스트\",\n");
        sb.append("  \"suggestionsByQuestionId\": {\n");
        sb.append("    \"1\": \"질문 1에 대한 제안 내용\",\n");
        sb.append("    \"2\": \"질문 2에 대한 제안 내용\"\n");
        sb.append("  }\n");
        sb.append("}");

        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", sb.toString()),
                Map.of("role", "user", "content", userMessageContent)
        );

        return webClient.post()
                .bodyValue(Map.of(
                        "model", "gpt-4o",
                        "messages", messages,
                        "temperature", 0.5
                ))
                .retrieve()
                .onStatus(status -> status.isError(), clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .doOnNext(errorBody -> log.error("[GPT 응답 에러 발생] 상태코드: {}, 응답 바디: \n{}",
                                        clientResponse.statusCode(), errorBody))
                                .flatMap(errorBody ->
                                        Mono.error(new RuntimeException("GPT 호출 실패: " + errorBody)))
                )
                .bodyToMono(OpenAIResponse.class)
                .doOnNext(response -> log.debug("[GPT 원 응답 JSON] \n{}", response))
                .handle((response, sink) -> {
                    String content = response.getChoices().get(0).getMessage().getContent();
                    try {
                        sink.next(new ObjectMapper().readValue(content, SurveyReportResponse.class));
                    } catch (JsonProcessingException e) {
                        sink.error(new RuntimeException("GPT 응답 파싱 실패: " + content, e));
                    }
                });
    }


    public Mono<String> summarizeDocument(String documentText) {
        if (documentText == null || documentText.isBlank()) {
            return Mono.just("|||요약할 내용이 없습니다.");
        }

        String prompt = "다음 텍스트는 하나의 연속된 문서입니다. 다음 두 가지 작업을 수행해주세요.\n" +
                "1. 이 안건지의 핵심 키워드(안건)를 추출해서 콤마(,)로 구분해주세요.\n" +
                "2. 전체 내용을 255자 이내의 짧은 문단으로 한국어로 요약해주세요.\n" +
                "반드시 '키워드1,키워드2,키워드3|||요약 내용' 형식으로 응답해주세요. 다른 설명은 붙이지 마세요." +
                "\n\n--- 문서 내용 ---\n" + documentText;

        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", "너는 주어진 안건지의 내용을 분석하여, 정해진 형식에 따라 키워드와 요약문을 반환하는 AI야."),
                Map.of("role", "user", "content", prompt)
        );

        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o",
                "messages", messages,
                "max_tokens", 400
        );

        return webClient.post()
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(OpenAIResponse.class)
                .map(response -> response.getChoices().get(0).getMessage().getContent());
    }

    public Mono<String> analyzeMeetingTranscript(String documentText, String agendaResult) {
        if (documentText == null || documentText.isBlank()) {
            return Mono.just("분석할 대화 내용이 없습니다.");
        }

        String prompt = "다음은 회의 전에 공유된 '안건 요약'과 실제 진행된 '회의 대화록'입니다. 두 내용을 모두 참고하여 다음 작업을 수행해주세요.\n\n" +
                "--- 안건 요약 ---\n" +
                (agendaResult != null && !agendaResult.isBlank() ? agendaResult : "제공된 안건 요약 없음") + "\n\n" +
                "--- 회의 대화록 ---\n" +
                documentText + "\n\n" +
                "회의록을 아래의 마크다운 형식에 맞춰 정리해주세요. 각 항목의 내용은 회의 대화록을 기반으로 채워주세요.\n" +
                "형식:\n" +
                "1. (회의의 첫 번째 핵심 주제)\n" +
                "    - (주제에 대한 세부 내용 또는 결정 사항 1)\n" +
                "    - (주제에 대한 세부 내용 또는 결정 사항 2)\n" +
                "2. (회의의 두 번째 핵심 주제)\n" +
                "    - (주제에 대한 세부 내용...)\n" +
                "(이후 논의된 핵심 주제가 있다면 3., 4. 형식으로 계속 이어서 작성해주세요.)\n" +
                "각 넘버링된 핵심 주제 아래에는 반드시 한 개 이상의 불릿포인트(-)가 포함되어야 합니다.\n" +
                "다른 설명 없이, 위 마크다운 형식의 결과만 반환해주세요.";

        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", "너는 회의록을 지정된 마크다운 형식으로 정리하는 전문 AI 비서야."),
                Map.of("role", "user", "content", prompt)
        );

        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o",
                "messages", messages,
                "max_tokens", 1000
        );

        return webClient.post()
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(OpenAIResponse.class)
                .map(response -> response.getChoices().get(0).getMessage().getContent());
    }

}
