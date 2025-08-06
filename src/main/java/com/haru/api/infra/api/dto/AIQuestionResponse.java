package com.haru.api.infra.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class AIQuestionResponse {
    private Long speechId;
    private List<String> questions;
}
