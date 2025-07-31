package com.haru.api.domain.workspace.controller;

import com.haru.api.domain.user.security.jwt.SecurityUtil;
import com.haru.api.domain.userWorkspace.dto.UserWorkspaceResponseDTO;
import com.haru.api.domain.userWorkspace.service.UserWorkspaceQueryService;
import com.haru.api.domain.workspace.dto.WorkspaceRequestDTO;
import com.haru.api.domain.workspace.dto.WorkspaceResponseDTO;
import com.haru.api.domain.workspace.service.WorkspaceCommandService;
import com.haru.api.domain.workspace.service.WorkspaceQueryService;
import com.haru.api.global.apiPayload.ApiResponse;
import com.haru.api.global.apiPayload.exception.handler.WorkspaceHandler;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {

    private final WorkspaceCommandService workspaceCommandService;
    private final UserWorkspaceQueryService userWorkspaceQueryService;
    private final WorkspaceQueryService workspaceQueryService;

    @Operation(
            summary = "워크스페이스 생성",
            description = "# [v1.0 (2025-07-31)](https://www.notion.so/workspace-2265da7802c5808e9405f37866203a43)" +
                    " 워크스페이스 생성 API 입니다. 워크스페이스 제목과 사진을 첨부해주세요."
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<WorkspaceResponseDTO.Workspace> createWorkspace(
            @RequestPart("request") @Validated WorkspaceRequestDTO.WorkspaceCreateRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {

        Long userId = SecurityUtil.getCurrentUserId();

        WorkspaceResponseDTO.Workspace workspace = workspaceCommandService.createWorkspace(userId, request, image);

        return ApiResponse.onSuccess(workspace);
    }

    @Operation(
            summary = "워크스페이스 리스트 제목 조회",
            description = "# [v1.0 (2025-07-31)](https://www.notion.so/workspace-2265da7802c5801e9c83f6675bbc9de7)" +
                    " 워크스페이스 리스트 제목 조회 API 입니다. jwt 토큰을 헤더에 넣어주세요"
    )
    @GetMapping("/me")
    public ApiResponse<List<UserWorkspaceResponseDTO.UserWorkspaceWithTitle>> getWorkspaceWithTitleList() {

        Long userId = SecurityUtil.getCurrentUserId();

        List<UserWorkspaceResponseDTO.UserWorkspaceWithTitle> workspaceWithTitleList = userWorkspaceQueryService.getUserWorkspaceList(userId);

        return ApiResponse.onSuccess(workspaceWithTitleList);
    }

    @Operation(
            summary = "워크스페이스 수정",
            description = "# [v1.1 (2025-07-31)](https://www.notion.so/workspace-2265da7802c580ebb332e868007671a7)" +
                    " 워크스페이스 수정 API 입니다. jwt 토큰을 헤더에 넣어주세요"
    )
    @PatchMapping(value = "/{workspaceId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<WorkspaceResponseDTO.Workspace> updateWorkspace(
            @RequestPart("request") @Validated WorkspaceRequestDTO.WorkspaceUpdateRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @PathVariable Long workspaceId
    ) {
        Long userId = SecurityUtil.getCurrentUserId();

        WorkspaceResponseDTO.Workspace workspace = workspaceCommandService.updateWorkspace(userId, workspaceId, request, image);

        return ApiResponse.onSuccess(workspace);
    }

    @Operation(
            summary = "워크스페이스 초대 수락",
            description = "# [v1.0 (2025-07-31)](https://www.notion.so/workspace-22e5da7802c580a3baf7c52a9fd8d45e?pvs=25)" +
                    " 워크스페이스 초대 수락 API 입니다."
    )
    @GetMapping("/invite-accept")
    public RedirectView acceptInvite(
            @RequestParam("token") String token
    ) {
        try {
            WorkspaceResponseDTO.InvitationAcceptResult result = workspaceCommandService.acceptInvite(token);

            String redirectUrl;
            if (result.isSuccess()) {
                if (result.isAlreadyRegistered()) {
                    // 이미 가입된 사용자면, 로그인 후 워크스페이스 페이지로 이동
                    redirectUrl = "https://haru.it.kr/auth/sign-in?redirect=/workspace/" + result.getWorkspaceId();
                } else {
                    // 미가입 사용자면 회원가입 페이지로 이동 (토큰 정보 포함)
                    redirectUrl = "https://haru.it.kr/auth/sign-up?token=" + token;
                }
            } else {
                redirectUrl = "https://haru.it.kr/error-page";
            }

            return new RedirectView(redirectUrl);
        } catch (WorkspaceHandler e) {
            return new RedirectView("https://haru.it.kr/error-page");
        }
    }

    @Operation(
            summary = "워크스페이스 문서 검색",
            description = "# [v1.0 (2025-07-31)](https://www.notion.so/2265da7802c580ca9a33eb9ba7ddec29?pvs=25)" +
                    " 워크스페이스 문서 검색 API 입니다. jwt 토큰을 헤더에 넣고, path variable로 workspaceId, query string에 문서 제목을 넣어주세요"
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

    @Operation(
            summary = "워크스페이스 초대 메일 발송",
            description = "# [v1.0 (2025-07-31)](https://www.notion.so/workspace-2385da7802c5804c86a5c1c7ca3b13cf?pvs=25)" +
                    " 워크스페이스 초대 메일 발송 API 입니다. jwt 토큰을 헤더에 넣어주세요"
    )
    @PostMapping("/invite") ApiResponse<Void> sendInviteEmail(
            @RequestBody WorkspaceRequestDTO.WorkspaceInviteEmailRequest request
    ) {
        Long userId = SecurityUtil.getCurrentUserId();

        workspaceCommandService.sendInviteEmail(userId, request);

        return ApiResponse.onSuccess(null);
    }

}
