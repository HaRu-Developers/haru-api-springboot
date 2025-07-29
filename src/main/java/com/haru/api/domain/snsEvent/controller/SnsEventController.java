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
}
