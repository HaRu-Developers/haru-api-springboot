package com.haru.api.domain.snsEvent.controller;

import com.haru.api.domain.snsEvent.dto.SnsEventRequestDTO;
import com.haru.api.domain.snsEvent.dto.SnsEventResponseDTO;
import com.haru.api.domain.snsEvent.entity.SnsEvent;
import com.haru.api.domain.snsEvent.entity.enums.Format;
import com.haru.api.domain.snsEvent.entity.enums.ListType;
import com.haru.api.domain.snsEvent.service.SnsEventCommandService;
import com.haru.api.domain.snsEvent.service.SnsEventQueryService;
import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.workspace.entity.Workspace;
import com.haru.api.global.annotation.AuthSnsEvent;
import com.haru.api.global.annotation.AuthUser;
import com.haru.api.global.annotation.AuthWorkspace;
import com.haru.api.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sns")
public class SnsEventController {

    private final SnsEventCommandService snsEventCommandService;
    private final SnsEventQueryService snsEventQueryService;

    @Operation(
            summary = "SNS 이벤트 생성 API [v1.0 (2025-08-05)]",
            description = " # [v1.0 (2025-08-05)](https://www.notion.so/2265da7802c580e8b883e3e4481fd61d?v=2265da7802c5816ab095000cc1ddadca&p=2265da7802c580c49467fe1b3b5d0766&pm=s)" +
                    " SNS 이벤트 생성 API입니다. Header에 access token을 넣고 Path Variable에는 workspaceId를 Request Body에 SNS 이벤트 정보를 담아 요청해주세요."
    )
    @PostMapping("/{workspaceId}")
    public ApiResponse<SnsEventResponseDTO.CreateSnsEventResponse> instagramOauthRedirectUri(
            @PathVariable String workspaceId,
            @RequestBody SnsEventRequestDTO.CreateSnsRequest request,
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(hidden = true) @AuthWorkspace Workspace workspace
    ) {
        return ApiResponse.onSuccess(
                snsEventCommandService.createSnsEvent(user, workspace, request)
        );
    }

    @Operation(
            summary = "[백엔드 테스트용 API] 인스타그램 연동 위한 redirect-uri, 테스트를 위한 code를 받기 위해 만든 API",
            description = "인스타그램 로그인 후 인증 서버가 리다이렉트시키는 redirect-uri입니다. 인스타그램 계정이름과 인스타그램 API호출에 필요한 Access Token을 발급받습니다."
    )
    @GetMapping("/oauth/callback")
    public ApiResponse<?> instagramRedirectUri(
            @RequestParam String code
    ) {
        System.out.println("Received code: " + code);
        return ApiResponse.onSuccess("");
    }

    @Operation(
            summary = "인스타그램 연동 API [v1.1 (2025-08-07)]",
            description = "# [v1.1 (2025-08-07)](https://www.notion.so/API-21e5da7802c581cca23dff937ac3f155?p=23f5da7802c5803b98abe74d511c2cf4&pm=s)" +
                    " 인스타그램 로그인 후 인증 서버로부터 받은 code를 header에 넣어주시고, workspaceId를 Path Variable로 넣어주세요."
    )
    @PostMapping("/{workspaceId}/link-instagram")
    public ApiResponse<SnsEventResponseDTO.LinkInstagramAccountResponse> linkInstagramAccount(
            @RequestHeader("code") String code,
            @PathVariable String workspaceId,
            @Parameter(hidden = true) @AuthWorkspace Workspace workspace
    ) {
        System.out.println("Received accessToken: " + code);
        return ApiResponse.onSuccess(
                snsEventCommandService.getInstagramAccessTokenAndAccount(code, workspace)
        );
    }

    @Operation(
            summary = "SNS 이벤트 리스트 조회 API [v1.0 (2025-08-05)]",
            description = "# [v1.0 (2025-08-05)](https://www.notion.so/2265da7802c580e8b883e3e4481fd61d?v=2265da7802c5816ab095000cc1ddadca&p=2265da7802c5806b8088c79d33ee9a52&pm=s)" +
                    " SNS 이벤트 리스트 조회 API입니다. Header에 access token을 넣고 Path Variable에는 workspaceId를 넣어 요청해주세요."
    )
    @GetMapping("/{workspaceId}/list")
    public ApiResponse<SnsEventResponseDTO.GetSnsEventListRequest> getSnsEventList(
            @PathVariable String workspaceId,
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(hidden = true) @AuthWorkspace Workspace workspace
    ) {

        return ApiResponse.onSuccess(
                snsEventQueryService.getSnsEventList(user, workspace)
        );
    }

    @Operation(
            summary = "SNS 이벤트명 수정 API [v1.0 (2025-08-05)]",
            description = "# [v1.0 (2025-08-05)](https://www.notion.so/2265da7802c580e8b883e3e4481fd61d?v=2265da7802c5816ab095000cc1ddadca&p=22a5da7802c580d3bed7c57de0b88492&pm=s)" +
                    " SNS 이벤트명 수정 API입니다. Header에 access token을 넣고 Path Variable에는 snsEvnetId를 Request Body에 SNS 이벤트 수정 정보(title)를 담아 요청해주세요."
    )
    @PatchMapping("/{snsEventId}")
    public ApiResponse<?> updateSnsEventTitle(
            @PathVariable String snsEventId,
            @RequestBody SnsEventRequestDTO.UpdateSnsEventRequest request,
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(hidden = true) @AuthSnsEvent SnsEvent snsEvent
    ) {

        snsEventCommandService.updateSnsEventTitle(user, snsEvent, request);

        return ApiResponse.onSuccess("");

    }

    @Operation(
            summary = "SNS 이벤트 삭제 API [v1.0 (2025-08-05)]",
            description = "# [v1.0 (2025-08-05)](https://www.notion.so/2265da7802c580e8b883e3e4481fd61d?v=2265da7802c5816ab095000cc1ddadca&p=2265da7802c5809b84d3d8c09f95c36b&pm=s)" +
                    " SNS 이벤트 삭제 API입니다. Header에 access token을 넣고 Path Variable에는 삭제할 SNS Event의 snsEvnetId를 담아 요청해주세요."
    )
    @DeleteMapping("/{snsEventId}")
    public ApiResponse<?> deleteSnsEvent(
            @PathVariable String snsEventId,
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(hidden = true) @AuthSnsEvent SnsEvent snsEvent

    ) {

        snsEventCommandService.deleteSnsEvent(user, snsEvent);

        return ApiResponse.onSuccess("");

    }

    @Operation(
            summary = "SNS 이벤트 조회 API [v1.0 (2025-08-05)]",
            description = "# [v1.0 (2025-08-05)](https://www.notion.so/2265da7802c580e8b883e3e4481fd61d?v=2265da7802c5816ab095000cc1ddadca&p=2265da7802c580c29f17d5bb447eb496&pm=s)" +
                    " SNS 이벤트 조회 API입니다. Header에 access token을 넣고 Path Variable에는 snsEventId를 넣어 요청해주세요."
    )
    @GetMapping("/{snsEventId}")
    public ApiResponse<SnsEventResponseDTO.GetSnsEventRequest> getSnsEvent(
            @PathVariable String snsEventId,
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(hidden = true) @AuthSnsEvent SnsEvent snsEvent
    ) {
        return ApiResponse.onSuccess(
                snsEventQueryService.getSnsEvent(user, snsEvent)
        );
    }

    @Operation(
            summary = "참여자 및 당첨자 리스트 다운로드 API [v1.0 (2025-08-11)]",
            description = "# [v1.0 (2025-08-11)](https://www.notion.so/API-21e5da7802c581cca23dff937ac3f155?p=2475da7802c5803ca84dc3f4b50ae257&pm=s)" +
                    " 참여자 및 당첨자 리스트 다운로드 API입니다. Header에 access token을 넣고 Path Variable에는 snsEventId를 넣어 요청해주세요. Query String에는 다운로드 형식을 넣어주시고 다운로드 형식이 docx라면 리스트의 HTML을 Request Body에 넣어주세요."
    )
    @PostMapping("/{snsEventId}/list/download")
    public ApiResponse<SnsEventResponseDTO.ListDownLoadLinkResponse> downloadList(
            @PathVariable String snsEventId,
            @RequestParam ListType listType,
            @RequestParam Format format,
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(hidden = true) @AuthSnsEvent SnsEvent snsEvent
    ) {
        return ApiResponse.onSuccess(
                snsEventCommandService.downloadList(
                        user,
                        snsEvent,
                        listType,
                        format
                )
        );
    }

    @Operation(
            summary = "워크스페이스에 연동된 인스타그램 계정 조회 API [v1.0 (2025-08-20)]",
            description = "# [v1.0 (2025-08-20)](https://www.notion.so/API-21e5da7802c581cca23dff937ac3f155?p=2545da7802c5801299c9f47578ba7d75&pm=s)" +
                    " 워크스페이스에 연동된 인스타그램 계정을 조회하는 API입니다. Path Variable에 workspaceId를 넣어 요청해주세요."
    )
    @GetMapping("/{workspaceId}/instagram")
    public ApiResponse<SnsEventResponseDTO.getInstagramAccountName> getInstagramAccountName(
            @PathVariable String workspaceId,
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(hidden = true) @AuthWorkspace Workspace workspace
    ) {
        return ApiResponse.onSuccess(
                snsEventQueryService.getInstagramAccountName(
                        user,
                        workspace
                )
        );
    }
}
