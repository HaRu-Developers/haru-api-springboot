package com.haru.api.domain.meeting.controller;

import com.haru.api.domain.meeting.dto.MeetingRequestDTO;
import com.haru.api.domain.meeting.dto.MeetingResponseDTO;
import com.haru.api.domain.meeting.service.MeetingCommandService;
import com.haru.api.domain.meeting.service.MeetingQueryService;
import com.haru.api.domain.user.security.jwt.SecurityUtil;
import com.haru.api.global.apiPayload.ApiResponse;
import com.haru.api.global.apiPayload.code.status.ErrorStatus;
import com.haru.api.global.apiPayload.exception.GeneralException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/meetings")
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingCommandService meetingCommandService;
    private final MeetingQueryService meetingQueryService;


    @Operation(summary = "회의 생성 API", description = "# [v1.1 (2025-08-05)] 안건지 파일과 회의 정보를 받아 회의를 생성합니다. accesstoken을 header에 입력해주세요",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            encoding = {
                                    @Encoding(name = "request", contentType = MediaType.APPLICATION_JSON_VALUE),
                                    @Encoding(name = "agendaFile", contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE)
                            }
                    )
            )
    )
    @PostMapping(
            consumes = {MediaType.MULTIPART_FORM_DATA_VALUE },
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ApiResponse<MeetingResponseDTO.createMeetingResponse> createMeeting(
            @RequestPart("agendaFile") MultipartFile agendaFile,
            @RequestPart("request") MeetingRequestDTO.createMeetingRequest request) {

        // file업로드가 되지 않는 경우 controller단에서 요청 처리
        if (agendaFile == null || agendaFile.isEmpty()) {
            throw new GeneralException(ErrorStatus.MEETING_FILE_NOT_FOUND);
        }
        Long userId = SecurityUtil.getCurrentUserId();

        MeetingResponseDTO.createMeetingResponse response = meetingCommandService.createMeeting(userId, agendaFile, request);

        return ApiResponse.onSuccess(response);
    }


    @Operation(summary = "AI회의록 list 조회", description =
            "# [v1.1 (2025-08-05)] workspaceId를 받아 회의록 list를 반환합니다. access token을 header에 입력해주세요."
    )
    @GetMapping("/workspaces/{workspaceId}")
    public ApiResponse<List<MeetingResponseDTO.getMeetingResponse>> getMeetings(
            @PathVariable("workspaceId") String workspaceId){

        Long userId = SecurityUtil.getCurrentUserId();

        List<MeetingResponseDTO.getMeetingResponse> response = meetingQueryService.getMeetings(userId, workspaceId);

        return ApiResponse.onSuccess(response);
    }

    @Operation(summary = "AI회의록 제목 수정", description =
            "# [v1.1 (2025-08-05)] meetingId을 pathparam, 수정할 title을 requestBody로 받아 회의록 제목을 수정핣니다. access token을 header에 입력해주세요."
    )
    @PatchMapping("/{meetingId}/title")
    public ApiResponse<String> updateMeetingTitle(
            @PathVariable("meetingId")String meetingId,
            @RequestBody MeetingRequestDTO.updateTitle request) {

        Long userId = SecurityUtil.getCurrentUserId();

        meetingCommandService.updateMeetingTitle(userId, meetingId, request.getTitle());

        return ApiResponse.onSuccess("제목수정이 완료되었습니다.");
    }

    @Operation(summary = "AI회의록 삭제", description =
            "# [v1.1 (2025-08-05)] meetingId를 받아 회의록을 삭제합니다. access token을 header에 입력해주세요."
    )
    @DeleteMapping("/{meetingId}")
    public ApiResponse<String> deleteMeeting(
            @PathVariable("meetingId") String meetingId) {

        Long userId = SecurityUtil.getCurrentUserId();

        meetingCommandService.deleteMeeting(userId, meetingId);

        return ApiResponse.onSuccess("회의가 삭제되었습니다.");
    }

    @Operation(summary = "AI회의록 단일조회", description =
            "# [v1.1 (2025-08-05)] meetingId를 받아 회의내용을 조회합니다. access token을 header에 입력해주세요."
    )
    @GetMapping("/{meetingId}/ai-proceeding")
    public ApiResponse<MeetingResponseDTO.getMeetingProceeding> getMeetingProceeding(
        @PathVariable("meetingId")String meetingId) {

        Long userId = SecurityUtil.getCurrentUserId();
        MeetingResponseDTO.getMeetingProceeding response = meetingQueryService.getMeetingProceeding(userId, meetingId);

        return ApiResponse.onSuccess(response);
    }




    @Operation(summary = "AI회의록 proceeding 수정", description =
            "# [v1.1 (2025-08-05)] meetingId와 수정된 Proceeding을 받아 회의록을 수정합니다. access token을 header에 입력해주세요."
    )
    @PatchMapping("/{meetingId}")
    public ApiResponse<String> adjustProceeding(
            @PathVariable("meetingId") String meetingId,
            @RequestBody MeetingRequestDTO.meetingProceedingRequest request) {

        Long userId = SecurityUtil.getCurrentUserId();

        meetingCommandService.adjustProceeding(userId, meetingId, request);

        return ApiResponse.onSuccess("회의가 수정되었습니다.");
    }
}
