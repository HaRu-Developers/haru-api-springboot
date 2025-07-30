package com.haru.api.domain.workspace.controller;

import com.haru.api.domain.user.security.jwt.SecurityUtil;
import com.haru.api.domain.userWorkspace.dto.UserWorkspaceResponseDTO;
import com.haru.api.domain.userWorkspace.service.UserWorkspaceQueryService;
import com.haru.api.domain.workspace.dto.WorkspaceRequestDTO;
import com.haru.api.domain.workspace.dto.WorkspaceResponseDTO;
import com.haru.api.domain.workspace.service.WorkspaceCommandService;
import com.haru.api.domain.workspace.service.WorkspaceQueryService;
import com.haru.api.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {

    private final WorkspaceCommandService workspaceCommandService;
    private final UserWorkspaceQueryService userWorkspaceQueryService;
    private final WorkspaceQueryService workspaceQueryService;

    @Operation(summary = "워크스페이스 생성", description =
            "# 워크스페이스 생성 API 입니다. 워크스페이스 제목과 초대하고자 하는 사람의 이메일을 입력해주세요."
    )
    @PostMapping
    public ApiResponse<WorkspaceResponseDTO.Workspace> createWorkspace(
            @RequestBody @Valid WorkspaceRequestDTO.WorkspaceCreateRequest request
    ) {

        Long userId = SecurityUtil.getCurrentUserId();

        WorkspaceResponseDTO.Workspace workspace = workspaceCommandService.createWorkspace(userId, request);

        return ApiResponse.onSuccess(workspace);
    }

    @Operation(summary = "워크스페이스 리스트 제목 조회", description =
            "# 워크스페이스 리스트 제목 조회 API 입니다. jwt 토큰을 헤더에 넣어주세요"
    )
    @GetMapping("/me")
    public ApiResponse<List<UserWorkspaceResponseDTO.UserWorkspaceWithTitle>> getWorkspaceWithTitleList() {

        Long userId = SecurityUtil.getCurrentUserId();

        List<UserWorkspaceResponseDTO.UserWorkspaceWithTitle> workspaceWithTitleList = userWorkspaceQueryService.getUserWorkspaceList(userId);

        return ApiResponse.onSuccess(workspaceWithTitleList);
    }

    @Operation(summary = "워크스페이스 수정", description =
            "# 워크스페이스 수정 API 입니다. jwt 토큰을 헤더에 넣어주세요"
    )
    @PatchMapping("/{workspaceId}")
    public ApiResponse<WorkspaceResponseDTO.Workspace> updateWorkspace(
            @RequestBody @Valid WorkspaceRequestDTO.WorkspaceUpdateRequest request,
            @PathVariable Long workspaceId
    ) {
        Long userId = SecurityUtil.getCurrentUserId();

        WorkspaceResponseDTO.Workspace workspace = workspaceCommandService.updateWorkspace(userId, workspaceId, request);

        return ApiResponse.onSuccess(workspace);
    }

    @Operation(summary = "워크스페이스 초대 수락", description =
            "# 워크스페이스 초대 수락 API 입니다. jwt 토큰을 헤더에 넣어주세요"
    )
    @PostMapping("/invite-accept")
    public ApiResponse<?> acceptInvite(
            @RequestParam("code") String code
    ) {
        workspaceCommandService.acceptInvite(code);

        return ApiResponse.onSuccess(null);
    }

    @Operation(summary = "워크스페이스 문서 검색", description =
            "# 워크스페이스 문서 검색 API 입니다. jwt 토큰을 헤더에 넣고, path variable로 workspaceId, query string에 문서 제목을 넣어주세요"
    )
    @GetMapping("/{workspaceId}")
    public ApiResponse<WorkspaceResponseDTO.DocumentList> getDocument(
            @PathVariable Long workspaceId,
            @RequestParam String title
    ) {
        Long userId = SecurityUtil.getCurrentUserId();

        WorkspaceResponseDTO.DocumentList documentList = workspaceQueryService.getDocuments(userId, workspaceId, title);

        return ApiResponse.onSuccess(documentList);
    }

    @Operation(summary = "워크스페이스 초대 메일 발송", description =
            "# 워크스페이스 초대 메일 발송 API 입니다. jwt 토큰을 헤더에 넣어주세요"
    )
    @PostMapping("/invite") ApiResponse<Void> sendInviteEmail(
            @RequestBody WorkspaceRequestDTO.WorkspaceInviteEmailRequest request
    ) {
        Long userId = SecurityUtil.getCurrentUserId();

        workspaceCommandService.sendInviteEmail(userId, request);

        return ApiResponse.onSuccess(null);
    }

}
