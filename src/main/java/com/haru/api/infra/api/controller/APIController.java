package com.haru.api.infra.api.controller;

import com.haru.api.infra.api.client.ChatGPTClient;
import com.haru.api.infra.api.dto.AIQuestionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/api-test")
@RequiredArgsConstructor
public class APIController {

    private final ChatGPTClient chatGPTClient;

    /**
     * ChatGPT APi 테스트용 컨트롤러
     */
    @PostMapping
    public String getChatGPTResponse(@RequestBody AIQuestionRequest request) {
        return chatGPTClient.getAIQuestionsRaw(request.getMeetingContent());
    }
}
