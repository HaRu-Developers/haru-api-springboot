package com.haru.api.infra.api.dto;

import lombok.*;

import java.util.Map;

@Data
public class SurveyReportResponse {
    // 전체 리포트 내용
    private String report;

    // 각 질문 ID별 suggestion
    private Map<Long, String> suggestionsByQuestionId;
}
