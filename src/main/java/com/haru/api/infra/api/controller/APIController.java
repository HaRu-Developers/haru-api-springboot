package com.haru.api.infra.api.controller;

import com.haru.api.infra.api.client.ChatGPTClient;
import com.haru.api.infra.api.dto.AIQuestionRequest;
import com.haru.api.infra.api.dto.SurveyReportRequest;
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
     * ChatGPT AI 회의 진행 매니져 질문 생성 API 테스트
     */
    @PostMapping("ai-question")
    public String getMeetingAIQuestionChatGPTResponse(@RequestBody AIQuestionRequest request) {
        return chatGPTClient.getAIQuestionsRaw(request.getMeetingContent());
    }

    /**
     * ChatGPT 팀 분위기 트래커 설문 리포트 생성 API 테스트
     */
    @PostMapping("report")
    public String getMoodTrackerReportChatGPTResponse(@RequestBody SurveyReportRequest request) {
        return chatGPTClient.getMoodTrackerReportRaw(request.getMoodTrackerContent());
    }
}
