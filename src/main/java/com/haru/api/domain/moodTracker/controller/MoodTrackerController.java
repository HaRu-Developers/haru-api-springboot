package com.haru.api.domain.moodTracker.controller;

import com.haru.api.domain.moodTracker.dto.MoodTrackerRequestDTO;
import com.haru.api.domain.moodTracker.dto.MoodTrackerResponseDTO;
import com.haru.api.domain.moodTracker.service.MoodTrackerCommandService;
import com.haru.api.domain.moodTracker.service.MoodTrackerQueryService;
import com.haru.api.domain.user.security.jwt.SecurityUtil;
import com.haru.api.global.apiPayload.code.status.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import com.haru.api.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/mood-trackers")
@RequiredArgsConstructor
public class MoodTrackerController {

    // DB 변경
    private final MoodTrackerCommandService moodTrackerCommandService;

    // 읽기 전용
    private final MoodTrackerQueryService moodTrackerQueryService;

    @GetMapping("/workspaces/{workspaceId}")
    @Operation(
            summary = "워크스페이스별 분위기 트래커 리스트 조회 API",
            description = "# 해당 워크스페이스(workspaceId)에 소속된 분위기 트래커 설문들을 모두 조회합니다."
    )
    @Parameters({
            @Parameter(name = "workspaceId", description = "워크스페이스 ID (Path Variable)", required = true)
    })
    public ApiResponse<List<MoodTrackerResponseDTO.Preview>> getMoodTrackersByWorkspace(
            @PathVariable Long workspaceId
    ) {
        List<MoodTrackerResponseDTO.Preview> result = moodTrackerQueryService.getMoodTrackerPreviewList(workspaceId);
        return ApiResponse.onSuccess(result);
    }

    @PostMapping("/workspaces/{workspaceId}")
    @Operation(
            summary = "분위기 트래커 설문 생성 API",
            description = "# 워크스페이스 ID, 설문 제목, 마감일 등을 입력받아 새로운 분위기 트래커 설문을 생성합니다."
    )
    @Parameters({
            @Parameter(name = "workspaceId", description = "워크스페이스 ID (Path Variable)", required = true)
    })
    public ApiResponse<MoodTrackerResponseDTO.CreateResult> createMoodTracker(
            @PathVariable Long workspaceId,
            @RequestBody @Valid MoodTrackerRequestDTO.CreateRequest request
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        MoodTrackerResponseDTO.CreateResult result = moodTrackerCommandService.create(userId, workspaceId,request);
        return ApiResponse.of(SuccessStatus.MOOD_TRACKER_CREATED, result);
    }

    @PatchMapping("/{mood-tracker-id}")
    @Operation(
            summary = "분위기 트래커 설문명 수정 API",
            description = "# 해당 ID의 분위기 트래커 설문 제목(title)을 수정합니다."
    )
    @Parameters({
            @Parameter(name = "mood-tracker-id", description = "분위기 트래커 ID (Path Variable)", required = true)
    })
    public ApiResponse<Void> updateMoodTrackerTitle(
            @PathVariable(name = "mood-tracker-id") Long moodTrackerId,
            @RequestBody @Valid MoodTrackerRequestDTO.UpdateTitleRequest request
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        moodTrackerCommandService.updateTitle(userId, moodTrackerId, request);
        return ApiResponse.of(SuccessStatus.MOOD_TRACKER_UPDATED, null);
    }

    @DeleteMapping("/{mood-tracker-id}")
    @Operation(
            summary = "분위기 트래커 설문 삭제 API",
            description = "# 해당 ID의 분위기 트래커 설문을 삭제합니다."
    )
    @Parameters({
            @Parameter(name = "mood-tracker-id", description = "분위기 트래커 ID (Path Variable)", required = true)
    })
    public ApiResponse<Void> deleteMoodTracker(
            @PathVariable(name = "mood-tracker-id") Long moodTrackerId
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        moodTrackerCommandService.delete(userId, moodTrackerId);
        return ApiResponse.of(SuccessStatus.MOOD_TRACKER_DELETED, null);
    }

}
