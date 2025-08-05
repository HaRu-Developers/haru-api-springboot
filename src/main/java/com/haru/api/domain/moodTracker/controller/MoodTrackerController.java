package com.haru.api.domain.moodTracker.controller;

import com.haru.api.domain.moodTracker.dto.MoodTrackerRequestDTO;
import com.haru.api.domain.moodTracker.dto.MoodTrackerResponseDTO;
import com.haru.api.domain.moodTracker.service.MoodTrackerCommandService;
import com.haru.api.domain.moodTracker.service.MoodTrackerQueryService;
import com.haru.api.domain.user.security.jwt.SecurityUtil;
import com.haru.api.global.apiPayload.code.status.SuccessStatus;
import com.haru.api.global.util.HashIdUtil;
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

    private final HashIdUtil hashIdUtil;

    @GetMapping("/workspaces/{workspaceId}")
    @Operation(
            summary = "워크스페이스별 분위기 트래커 리스트 조회 API",
            description = "# [v1.0 (2025-07-26)](https://www.notion.so/2265da7802c580048e63f104d98d7637) 해당 워크스페이스(workspaceId)에 소속된 분위기 트래커 설문들을 모두 조회합니다."
    )
    @Parameters({
            @Parameter(name = "workspaceId", description = "워크스페이스 ID (Path Variable)", required = true)
    })
    public ApiResponse<MoodTrackerResponseDTO.PreviewList> getMoodTrackerPreviewListByWorkspace(
            @PathVariable Long workspaceId
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        MoodTrackerResponseDTO.PreviewList result = moodTrackerQueryService.getMoodTrackerPreviewList(userId, workspaceId);
        return ApiResponse.onSuccess(result);
    }

    @PostMapping("/workspaces/{workspaceId}")
    @Operation(
            summary = "분위기 트래커 설문 생성 API",
            description = "# [v1.0 (2025-07-26)](https://www.notion.so/2265da7802c580429bd5ed5067cbe5ba) 워크스페이스 ID, 설문 제목, 마감일 등을 입력받아 새로운 분위기 트래커 설문을 생성합니다."
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

    @PatchMapping("/{mood-tracker-hashed-Id}")
    @Operation(
            summary = "분위기 트래커 설문명 수정 API",
            description = "# [v1.0 (2025-07-26)](https://www.notion.so/22a5da7802c580fe80ece5981e90b03b) 해당 ID의 분위기 트래커 설문 제목(title)을 수정합니다."
    )
    @Parameters({
            @Parameter(name = "mood-tracker-hashed-Id", description = "해시된 16자 분위기 트래커 ID (Path Variable)", required = true)
    })
    public ApiResponse<Void> updateMoodTrackerTitle(
            @PathVariable(name = "mood-tracker-hashed-Id") String moodTrackerHashedId,
            @RequestBody @Valid MoodTrackerRequestDTO.UpdateTitleRequest request
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        Long moodTrackerId = hashIdUtil.decode(moodTrackerHashedId);
        moodTrackerCommandService.updateTitle(userId, moodTrackerId, request);
        return ApiResponse.of(SuccessStatus.MOOD_TRACKER_UPDATED, null);
    }

    @DeleteMapping("/{mood-tracker-hashed-Id}")
    @Operation(
            summary = "분위기 트래커 설문 삭제 API",
            description = "# [v1.0 (2025-07-26)](https://www.notion.so/2265da7802c58011aa54ea2c1818ef04) 해당 ID의 분위기 트래커 설문을 삭제합니다."
    )
    @Parameters({
            @Parameter(name = "mood-tracker-hashed-Id", description = "해시된 16자 분위기 트래커 ID (Path Variable)", required = true)
    })
    public ApiResponse<Void> deleteMoodTracker(
            @PathVariable(name = "mood-tracker-hashed-Id") String moodTrackerHashedId
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        Long moodTrackerId = hashIdUtil.decode(moodTrackerHashedId);
        moodTrackerCommandService.delete(userId, moodTrackerId);
        return ApiResponse.of(SuccessStatus.MOOD_TRACKER_DELETED, null);
    }

    @PostMapping("/{mood-tracker-hashed-Id}/emails")
    @Operation(
            summary = "분위기 트래커 설문 링크 워크 스페이스 내의 팀원 email 전송 API",
            description = "# [v1.0 (2025-07-26)](https://www.notion.so/22a5da7802c580a799cec13c005824d7) 해당 ID의 분위기 트래커 설문 링크를 워크 스페이스 내의 유저에게 email로 전송합니다."
    )
    @Parameters({
            @Parameter(name = "mood-tracker-hashed-Id", description = "해시된 16자 분위기 트래커 ID (Path Variable)", required = true)
    })
    public ApiResponse<Void> sendMoodTrackerSurveyLink(
            @PathVariable(name = "mood-tracker-hashed-Id") String moodTrackerHashedId
    ) {
        Long moodTrackerId = hashIdUtil.decode(moodTrackerHashedId);
        moodTrackerCommandService.sendSurveyLink(moodTrackerId);
        return ApiResponse.of(SuccessStatus.MOOD_TRACKER_EMAIL_SENT, null);
    }

    @PostMapping("/{mood-tracker-hashed-Id}/answer")
    @Operation(
            summary = "분위기 트래커 설문 답변 제출 API",
            description = "# [v1.1 (2025-08-05)](https://www.notion.so/2265da7802c580c58d36e73639e41291) 해당 ID의 분위기 트래커 설문 답변을 제출합니다."
    )
    @Parameters({
            @Parameter(name = "mood-tracker-hashed-Id", description = "해시된 16자 분위기 트래커 ID (Path Variable)", required = true)
    })
    public  ApiResponse<Void> submitMoodTrackerSurveyAnswers(
            @PathVariable("mood-tracker-hashed-Id") String moodTrackerHashedId,
            @RequestBody MoodTrackerRequestDTO.SurveyAnswerList request
    ) {
        Long moodTrackerId = hashIdUtil.decode(moodTrackerHashedId);
        moodTrackerCommandService.submitSurveyAnswers(moodTrackerId, request);
        return ApiResponse.of(SuccessStatus.MOOD_TRACKER_ANSWER_SUBMIT, null);
    }

    @PostMapping("/{mood-tracker-hashed-Id}/report-test")
    @Operation(
            summary = "분위기 트래커 설문 리포트 즉시 생성 테스트 API",
            description = "# [v1.0 (2025-07-26)](https://www.notion.so/23f5da7802c58080b4a5e6d24b47d924) 해당 ID의 분위기 트래커 설문 리포트를 즉시 생성합니다."
    )
    @Parameters({
            @Parameter(name = "mood-tracker-hashed-Id", description = "해시된 16자 분위기 트래커 ID (Path Variable)", required = true)
    })
    public  ApiResponse<Void> generateMoodTrackerReportTest (
            @PathVariable("mood-tracker-hashed-Id") String moodTrackerHashedId
    ) {
        Long moodTrackerId = hashIdUtil.decode(moodTrackerHashedId);
        moodTrackerCommandService.generateReportTest(moodTrackerId);
        return ApiResponse.of(SuccessStatus._OK, null);
    }

    @GetMapping("/{mood-tracker-hashed-Id}/questions")
    @Operation(
            summary = "분위기 트래커 설문 문항 조회 API",
            description = "# [v1.2 (2025-08-05)](https://www.notion.so/2295da7802c580dbb88aee8687b69e32) 분위기 트래커(moodTrackerId)에 해당하는 설문 문항들을 조회합니다."
    )
    @Parameters({
            @Parameter(name = "mood-tracker-hashed-Id", description = "분위기 트래커 ID (Hashed, Path Variable)", required = true)
    })
    public ApiResponse<MoodTrackerResponseDTO.QuestionResult> getMoodTrackerQuestionResult(
            @PathVariable(name = "mood-tracker-hashed-Id") String moodTrackerHashedId
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        Long moodTrackerId = hashIdUtil.decode(moodTrackerHashedId);
        MoodTrackerResponseDTO.QuestionResult result = moodTrackerQueryService.getQuestionResult(userId, moodTrackerId);
        return ApiResponse.onSuccess(result);
    }

    @GetMapping("/{mood-tracker-hashed-Id}/reports")
    @Operation(
            summary = "분위기 트래커 설문 팀분위기 리포트 조회 API",
            description = "# [v1.2 (2025-08-05)](https://www.notion.so/2295da7802c580ba8401c449389e8f78) 분위기 트래커(moodTrackerId)에 대한 팀 전체 리포트를 조회합니다."
    )
    @Parameters({
            @Parameter(name = "mood-tracker-hashed-Id", description = "분위기 트래커 ID (Hashed, Path Variable)", required = true)
    })
    public ApiResponse<MoodTrackerResponseDTO.ReportResult> getMoodTrackerReportResult(
            @PathVariable(name = "mood-tracker-hashed-Id") String moodTrackerHashedId
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        Long moodTrackerId = hashIdUtil.decode(moodTrackerHashedId);
        MoodTrackerResponseDTO.ReportResult result = moodTrackerQueryService.getReportResult(userId, moodTrackerId);
        return ApiResponse.onSuccess(result);
    }

    @GetMapping("/{mood-tracker-hashed-Id}/responses")
    @Operation(
            summary = "분위기 트래커 설문 응답 조회 API",
            description = "# [v1.2 (2025-08-05)](https://www.notion.so/2265da7802c5808290adf17d8d4591a4) 분위기 트래커(moodTrackerId)에 대한 개별 응답 데이터를 조회합니다."
    )
    @Parameters({
            @Parameter(name = "mood-tracker-hashed-Id", description = "분위기 트래커 ID (Hashed, Path Variable)", required = true)
    })
    public ApiResponse<MoodTrackerResponseDTO.ResponseResult> getMoodTrackerResponseResult(
            @PathVariable(name = "mood-tracker-hashed-Id") String moodTrackerHashedId
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        Long moodTrackerId = hashIdUtil.decode(moodTrackerHashedId);
        MoodTrackerResponseDTO.ResponseResult result = moodTrackerQueryService.getResponseResult(userId, moodTrackerId);
        return ApiResponse.onSuccess(result);
    }
}
