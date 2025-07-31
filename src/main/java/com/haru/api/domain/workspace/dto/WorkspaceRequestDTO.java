package com.haru.api.domain.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class WorkspaceRequestDTO {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkspaceCreateRequest {
        @NotBlank(message = "워크스페이스 제목은 빈 값일 수 없습니다.")
        private String title;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkspaceUpdateRequest {
        @NotBlank(message = "수정하고자 하는 제목은 빈 값일 수 없습니다.")
        private String title;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkspaceInviteEmailRequest {
        private Long workspaceId;
        private List<String> emails;
    }
}
