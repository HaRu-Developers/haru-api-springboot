package com.haru.api.domain.workspace.controller;

import com.haru.api.domain.user.security.jwt.SecurityUtil;
import com.haru.api.domain.workspace.dto.WorkspaceRequestDTO;
import com.haru.api.domain.workspace.dto.WorkspaceResponseDTO;
import com.haru.api.domain.workspace.service.WorkspaceCommandService;
import com.haru.api.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {

    private final WorkspaceCommandService workspaceCommandService;

    @Operation(summary = "워크스페이스 생성", description =
            "# 워크스페이스 생성 API 입니다. 워크스페이스 제목과 초대하고자 하는 사람의 이메일을 입력해주세요."
    )
    @PostMapping
    public ApiResponse<WorkspaceResponseDTO.Workspace> createWorkspace(
            @RequestBody WorkspaceRequestDTO.WorkspaceCreateRequest request
    ) {

        Long userId = SecurityUtil.getCurrentUserId();

        WorkspaceResponseDTO.Workspace workspace = workspaceCommandService.createWorkspace(userId, request);

        return ApiResponse.onSuccess(workspace);
    }

}
