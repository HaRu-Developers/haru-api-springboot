package com.haru.api.domain.snsEvent.controller;

import com.haru.api.domain.snsEvent.dto.SnsEventRequestDTO;
import com.haru.api.domain.snsEvent.dto.SnsEventResponseDTO;
import com.haru.api.domain.snsEvent.service.SnsEventCommandService;
import com.haru.api.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sns")
public class SnsEventController {

    private final SnsEventCommandService snsEventCommandService;

    @Operation(
            summary = "SNS 이벤트 생성 API",
            description = "SNS 이벤트 생성 API입니다. Header에 access token을 넣고 Path Variable에는 workspaceId를 Request Body에 SNS 이벤트 정보를 담아 요청해주세요."
    )
    @PostMapping("/{workspaceId}")
    public ApiResponse<SnsEventResponseDTO.CreateSnsEventResponse> instagramOauthRedirectUri(
            @PathVariable Long workspaceId,
            @RequestBody SnsEventRequestDTO.CreateSnsRequest request
    ) {
        return ApiResponse.onSuccess(
                snsEventCommandService.createSnsEvent(workspaceId, request)
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
            summary = "인스타그램 연동 API",
            description = "인스타그램 로그인 후 인증 서버로부터 받은 code를 header에 넣어주시고, workspaceId를 Path Variable로 넣어주세요."
    )
    @GetMapping("/{workspaceId}/link-instagram")
    public ApiResponse<SnsEventResponseDTO.LinkInstagramAccountResponse> linkInstagramAccount(
            @RequestHeader("AccessToken") String accessToken,
            @PathVariable Long workspaceId
    ) {
        System.out.println("Received accessToken: " + accessToken);
        return ApiResponse.onSuccess(
                snsEventCommandService.getInstagramAccessTokenAndAccount(accessToken, workspaceId)
        );
    }
}
