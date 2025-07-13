package com.haru.api.domain.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

public class WorkspaceRequestDTO {

    @Getter
    @Builder
    public static class WorkspaceCreateRequest {
        @NotBlank(message = "워크스페이스 제목은 빈 값일 수 없습니다.")
        private String name;
        private List<String> memberEmails;
    }
}
