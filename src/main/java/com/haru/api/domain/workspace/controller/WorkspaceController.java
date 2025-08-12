package com.haru.api.domain.workspace.controller;

import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.userWorkspace.dto.UserWorkspaceResponseDTO;
import com.haru.api.domain.userWorkspace.service.UserWorkspaceQueryService;
import com.haru.api.domain.workspace.dto.WorkspaceRequestDTO;
import com.haru.api.domain.workspace.dto.WorkspaceResponseDTO;
import com.haru.api.domain.workspace.entity.Workspace;
import com.haru.api.domain.workspace.service.WorkspaceCommandService;
import com.haru.api.domain.workspace.service.WorkspaceQueryService;
import com.haru.api.global.annotation.AuthUser;
import com.haru.api.global.annotation.AuthWorkspace;
import com.haru.api.global.apiPayload.ApiResponse;
import com.haru.api.global.apiPayload.exception.handler.WorkspaceHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.view.RedirectView;

import java.time.LocalDate;
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
            @RequestPart(value = "image", required = false) MultipartFile image,
            @Parameter(hidden = true) @AuthUser User user
    ) {
        WorkspaceResponseDTO.Workspace workspace = workspaceCommandService.createWorkspace(user, request, image);

        return ApiResponse.onSuccess(workspace);
    }

    @Operation(
            summary = "워크스페이스 리스트 제목 조회",
            description = "# [v1.0 (2025-07-31)](https://www.notion.so/workspace-2265da7802c5801e9c83f6675bbc9de7)" +
                    " 워크스페이스 리스트 제목 조회 API 입니다. jwt 토큰을 헤더에 넣어주세요"
    )
    @GetMapping("/me")
    public ApiResponse<List<UserWorkspaceResponseDTO.UserWorkspaceWithTitle>> getWorkspaceWithTitleList(
            @Parameter(hidden = true) @AuthUser User user
    ) {
        List<UserWorkspaceResponseDTO.UserWorkspaceWithTitle> workspaceWithTitleList = userWorkspaceQueryService.getUserWorkspaceList(user);

        return ApiResponse.onSuccess(workspaceWithTitleList);
    }

    @Operation(
            summary = "워크스페이스 수정",
            description = "# [v1.2 (2025-08-05)](https://www.notion.so/workspace-2265da7802c580ebb332e868007671a7)" +
                    " 워크스페이스 수정 API 입니다. jwt 토큰을 헤더에 넣어주세요"
    )
    @PatchMapping(value = "/{workspaceId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<WorkspaceResponseDTO.Workspace> updateWorkspace(
            @RequestPart("request") @Validated WorkspaceRequestDTO.WorkspaceUpdateRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @PathVariable String workspaceId,
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(hidden = true) @AuthWorkspace Workspace workspace
    ) {
        WorkspaceResponseDTO.Workspace updatedWorkspace = workspaceCommandService.updateWorkspace(user, workspace, request, image);

        return ApiResponse.onSuccess(updatedWorkspace);
    }

    @Operation(
            summary = "워크스페이스 초대 수락",
            description = "# [v1.0 (2025-07-31)](https://www.notion.so/workspace-22e5da7802c580a3baf7c52a9fd8d45e)" +
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
            description = "# [v1.1 (2025-08-05)](https://www.notion.so/2265da7802c580ca9a33eb9ba7ddec29)" +
                    " 워크스페이스 문서 검색 API 입니다. jwt 토큰을 헤더에 넣고, path variable로 workspaceId, query string에 문서 제목을 넣어주세요"
    )
    @GetMapping("/{workspaceId}")
    public ApiResponse<WorkspaceResponseDTO.DocumentList> getDocument(
            @PathVariable String workspaceId,
            @RequestParam String title,
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(hidden = true) @AuthWorkspace Workspace workspace

    ) {
        WorkspaceResponseDTO.DocumentList documentList = workspaceQueryService.getDocuments(user, workspace, title);

        return ApiResponse.onSuccess(documentList);
    }

    @Operation(
            summary = "워크스페이스 초대 메일 발송",
            description = "# [v1.0 (2025-07-31)](https://www.notion.so/workspace-2385da7802c5804c86a5c1c7ca3b13cf)" +
                    " 워크스페이스 초대 메일 발송 API 입니다. jwt 토큰을 헤더에 넣어주세요"
    )
    @PostMapping("/invite") ApiResponse<Void> sendInviteEmail(
            @RequestBody WorkspaceRequestDTO.WorkspaceInviteEmailRequest request,
            @Parameter(hidden = true) @AuthUser User user
    ) {
        workspaceCommandService.sendInviteEmail(user, request);

        return ApiResponse.onSuccess(null);
    }

    @Operation(
            summary = "사이드바 최근 문서 조회",
            description = "# [v1.1 (2025-08-05)](https://www.notion.so/22a5da7802c58014b70fce5cde93e3f2)" +
                    " 사이드바 최근 문서 조회 API 입니다. jwt 토큰을 헤더에 넣어주세요"
    )
    @GetMapping("/{workspaceId}/sidebar")
    public ApiResponse<WorkspaceResponseDTO.DocumentSidebarList> getSidebar(
            @PathVariable String workspaceId,
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(hidden = true) @AuthWorkspace Workspace workspace
    ) {
        WorkspaceResponseDTO.DocumentSidebarList documentSidebarList = workspaceQueryService.getDocumentWithoutLastOpenedList(user, workspace);

        return ApiResponse.onSuccess(documentSidebarList);
    }

    @Operation(
            summary = "캘린더 조회",
            description = "# [v1.1 (2025-08-05)](https://www.notion.so/2265da7802c58072bb65d4b17f6ef785?v=2265da7802c5816ab095000cc1ddadca)" +
                    " 캘린더 조회 API 입니다. jwt 토큰을 헤더에 넣고, path variable에 workspaceId, query string으로 시작 날짜, 종료 날짜를 넘겨주세요"
    )
    @GetMapping("/{workspaceId}/calendar")
    public ApiResponse<WorkspaceResponseDTO.DocumentCalendarList> getCalendar(
            @PathVariable String workspaceId,
            @RequestParam("start")LocalDate startDate,
            @RequestParam("end") LocalDate endDate,
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(hidden = true) @AuthWorkspace Workspace workspace
    ) {
        WorkspaceResponseDTO.DocumentCalendarList documentCalendarList = workspaceQueryService.getDocumentForCalendar(user, workspace, startDate, endDate);

        return ApiResponse.onSuccess(documentCalendarList);
    }

    @Operation(
            summary = "workspace 수정 페이지 조회",
            description = "# [v1.0 (2025-08-05)](https://www.notion.so/workspace-2465da7802c580ff853dd590d3672246)" +
                    " 워크스페이스 수정 페이지 조회 API 입니다. jwt 토큰을 헤더에 넣고, path variable에 workspaceId를 넘겨주세요"
    )
    @GetMapping("/{workspaceId}/edit")
    public ApiResponse<WorkspaceResponseDTO.WorkspaceEditPage> modifyWorkspace(
            @PathVariable String workspaceId,
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(hidden = true) @AuthWorkspace Workspace workspace
    ) {

        WorkspaceResponseDTO.WorkspaceEditPage workspaceEditPage = workspaceQueryService.getEditPage(user, workspace);

        return ApiResponse.onSuccess(workspaceEditPage);
    }


}
