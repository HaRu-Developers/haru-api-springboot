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


    @Operation(summary = "회의 생성 API", description = "안건지 파일과 회의 정보를 받아 회의를 생성합니다. accesstoken을 header에 입력해주세요",
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

    @GetMapping("/workspaces/{workspaceId}")
    public ApiResponse<List<MeetingResponseDTO.getMeetingResponse>> getMeetings(
            @PathVariable("workspaceId") Long workspaceId){

        Long userId = SecurityUtil.getCurrentUserId();

        List<MeetingResponseDTO.getMeetingResponse> response = meetingQueryService.getMeetings(userId, workspaceId);

        return ApiResponse.onSuccess(response);
    }

    @PatchMapping("/{meetingId}/title")
    public ApiResponse<String> updateMeetingTitle(
            @PathVariable("meetingId")Long meetingId,
            @RequestBody MeetingRequestDTO.updateTitle request) {

        Long userId = SecurityUtil.getCurrentUserId();

        meetingCommandService.updateMeetingTitle(userId, meetingId, request.getTitle());

        return ApiResponse.onSuccess("제목수정이 완료되었습니다.");
    }

    @DeleteMapping("/{meetingId}")
    public ApiResponse<String> deleteMeeting(
            @PathVariable("meetingId") Long meetingId) {

        Long userId = SecurityUtil.getCurrentUserId();

        meetingCommandService.deleteMeeting(userId, meetingId);

        return ApiResponse.onSuccess("회의가 삭제되었습니다.");
    }
}
