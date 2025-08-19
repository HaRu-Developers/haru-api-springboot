package com.haru.api.domain.moodTracker.controller;

import com.haru.api.domain.moodTracker.dto.MoodTrackerRequestDTO;
import com.haru.api.domain.moodTracker.dto.MoodTrackerResponseDTO;
import com.haru.api.domain.moodTracker.entity.MoodTracker;
import com.haru.api.domain.moodTracker.service.MoodTrackerCommandService;
import com.haru.api.domain.moodTracker.service.MoodTrackerQueryService;
import com.haru.api.domain.snsEvent.entity.enums.Format;
import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.workspace.entity.Workspace;
import com.haru.api.global.annotation.AuthMoodTracker;
import com.haru.api.global.annotation.AuthUser;
import com.haru.api.global.annotation.AuthWorkspace;
import com.haru.api.global.apiPayload.code.status.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import com.haru.api.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
            description = "# [v1.1 (2025-08-05)](https://www.notion.so/2265da7802c580048e63f104d98d7637) 해당 워크스페이스(workspaceId)에 소속된 분위기 트래커 설문들을 모두 조회합니다."
    )
    @Parameters({
            @Parameter(name = "workspaceId", description = "워크스페이스 ID (Path Variable)", required = true)
    })
    public ApiResponse<MoodTrackerResponseDTO.PreviewList> getMoodTrackerPreviewListByWorkspace(
            @PathVariable String workspaceId,
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(hidden = true) @AuthWorkspace Workspace workspace
    ) {

        MoodTrackerResponseDTO.PreviewList result = moodTrackerQueryService.getMoodTrackerPreviewList(user, workspace);

        return ApiResponse.onSuccess(result);

    }

    @PostMapping("/workspaces/{workspaceId}")
    @Operation(
            summary = "분위기 트래커 설문 생성 API",
            description = "# [v1.1 (2025-08-05)](https://www.notion.so/2265da7802c580429bd5ed5067cbe5ba) 워크스페이스 ID, 설문 제목, 마감일 등을 입력받아 새로운 분위기 트래커 설문을 생성합니다."
    )
    @Parameters({
            @Parameter(name = "workspaceId", description = "워크스페이스 ID (Path Variable)", required = true)
    })
    public ApiResponse<MoodTrackerResponseDTO.CreateResult> createMoodTracker(
            @PathVariable String workspaceId,
            @RequestBody @Valid MoodTrackerRequestDTO.CreateRequest request,
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(hidden = true) @AuthWorkspace Workspace workspace
    ) {

        MoodTrackerResponseDTO.CreateResult result = moodTrackerCommandService.create(user, workspace,request);

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
            @RequestBody @Valid MoodTrackerRequestDTO.UpdateTitleRequest request,
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(hidden = true) @AuthMoodTracker MoodTracker moodTracker
    ) {

        moodTrackerCommandService.updateTitle(user, moodTracker, request);

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
            @PathVariable(name = "mood-tracker-hashed-Id") String moodTrackerHashedId,
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(hidden = true) @AuthMoodTracker MoodTracker moodTracker
    ) {

        moodTrackerCommandService.delete(user, moodTracker);

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
            @PathVariable(name = "mood-tracker-hashed-Id") String moodTrackerHashedId,
            @Parameter(hidden = true) @AuthMoodTracker MoodTracker moodTracker
    ) {

        moodTrackerCommandService.sendSurveyLink(moodTracker);

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
            @RequestBody MoodTrackerRequestDTO.SurveyAnswerList request,
            @Parameter(hidden = true) @AuthMoodTracker MoodTracker moodTracker
    ) {

        moodTrackerCommandService.submitSurveyAnswers(moodTracker, request);

        return ApiResponse.of(SuccessStatus.MOOD_TRACKER_ANSWER_SUBMIT, null);

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
            @PathVariable(name = "mood-tracker-hashed-Id") String moodTrackerHashedId,
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(hidden = true) @AuthMoodTracker MoodTracker moodTracker
    ) {

        MoodTrackerResponseDTO.QuestionResult result = moodTrackerQueryService.getQuestionResult(user, moodTracker);

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
            @PathVariable(name = "mood-tracker-hashed-Id") String moodTrackerHashedId,
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(hidden = true) @AuthMoodTracker MoodTracker moodTracker
    ) {

        MoodTrackerResponseDTO.ReportResult result = moodTrackerQueryService.getReportResult(user, moodTracker);

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
            @PathVariable(name = "mood-tracker-hashed-Id") String moodTrackerHashedId,
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(hidden = true) @AuthMoodTracker MoodTracker moodTracker
    ) {

        MoodTrackerResponseDTO.ResponseResult result = moodTrackerQueryService.getResponseResult(user, moodTracker);

        return ApiResponse.onSuccess(result);

    }

    @GetMapping("/{mood-tracker-hashed-Id}/download")
    @Operation(
            summary = "분위기 트래커 설문 리포트 다운로드 API",
            description = "# [v1.0 (2025-08-13)](https://www.notion.so/2265da7802c580e3b53ddd8d181922b1) 분위기 트래커(moodTrackerId)에 대한 개별 리포트 다운로드 링크를 조회합니다."
    )
    @Parameters({
            @Parameter(name = "mood-tracker-hashed-Id", description = "분위기 트래커 ID (Hashed, Path Variable)", required = true)
    })
    public ApiResponse<MoodTrackerResponseDTO.ReportDownLoadLinkResponse> downloadList(
            @PathVariable(name = "mood-tracker-hashed-Id") String moodTrackerHashedId,
            @RequestParam Format format,
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(hidden = true) @AuthMoodTracker MoodTracker moodTracker
    ) {

        MoodTrackerResponseDTO.ReportDownLoadLinkResponse result = moodTrackerCommandService.getDownloadLink(user, moodTracker, format);

        return ApiResponse.onSuccess(result);

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
            @PathVariable("mood-tracker-hashed-Id") String moodTrackerHashedId,
            @Parameter(hidden = true) @AuthMoodTracker MoodTracker moodTracker
    ) {

        moodTrackerCommandService.generateReportTest(moodTracker);

        return ApiResponse.of(SuccessStatus._OK, null);

    }

    @PostMapping("/{mood-tracker-hashed-Id}/report-file-thumbnail-test")
    @Operation(
            summary = "분위기 트래커 설문 리포트, 파일, 썸네일 즉시 생성 테스트 API",
            description = "# [v1.0 (2025-08-14)](https://www.notion.so/24f5da7802c58019a1f7d9c8e882226e) 해당 ID의 분위기 트래커 설문 리포트를 즉시 생성합니다."
    )
    @Parameters({
            @Parameter(name = "mood-tracker-hashed-Id", description = "해시된 16자 분위기 트래커 ID (Path Variable)", required = true)
    })
    public  ApiResponse<Void> generateMoodTrackerReportFileAndThumbnailTest (
            @PathVariable("mood-tracker-hashed-Id") String moodTrackerHashedId,
            @Parameter(hidden = true) @AuthMoodTracker MoodTracker moodTracker
    ) {

        moodTrackerCommandService.generateReportFileAndThumbnailTest(moodTracker);

        return ApiResponse.of(SuccessStatus._OK, null);

    }
}
