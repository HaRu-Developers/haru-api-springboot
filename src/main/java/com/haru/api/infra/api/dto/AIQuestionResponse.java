package com.haru.api.infra.api.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.List;

@Data
public class AIQuestionResponse {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long speechId;
    private List<String> questions;
}
