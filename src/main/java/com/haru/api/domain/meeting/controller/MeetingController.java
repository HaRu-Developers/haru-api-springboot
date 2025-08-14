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


    @Operation(summary = "회의 생성 API", description = "# [v1.1 (2025-08-05)](https://www.notion.so/2265da7802c580e8bf25c99cc81998bb)" +" 안건지 파일과 회의 정보를 받아 회의를 생성합니다. accesstoken을 header에 입력해주세요",
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
            throw new GeneralException(ErrorStatus.MEETING_AGENDAFILE_NOT_FOUND);
        }
        Long userId = SecurityUtil.getCurrentUserId();

        MeetingResponseDTO.createMeetingResponse response = meetingCommandService.createMeeting(userId, agendaFile, request);

        return ApiResponse.onSuccess(response);
    }


    @Operation(summary = "AI회의록 list 조회", description =
            "# [v1.1 (2025-08-05)](https://www.notion.so/2265da7802c580de8b79d0b2b5061ddf)"+" workspaceId를 받아 회의록 list를 반환합니다. access token을 header에 입력해주세요."
    )
    @GetMapping("/workspaces/{workspaceId}")
    public ApiResponse<List<MeetingResponseDTO.getMeetingResponse>> getMeetings(
            @PathVariable("workspaceId") String workspaceId){

        Long userId = SecurityUtil.getCurrentUserId();

        List<MeetingResponseDTO.getMeetingResponse> response = meetingQueryService.getMeetings(userId, Long.parseLong(workspaceId));

        return ApiResponse.onSuccess(response);
    }

    @Operation(summary = "AI회의록 제목 수정", description =
            "# [v1.1 (2025-08-05)](https://www.notion.so/22a5da7802c5807c8f1cef8f65a61bca)"+" meetingId을 pathparam, 수정할 title을 requestBody로 받아 회의록 제목을 수정핣니다. access token을 header에 입력해주세요."
    )
    @PatchMapping("/{meetingId}/title")
    public ApiResponse<String> updateMeetingTitle(
            @PathVariable("meetingId")String meetingId,
            @RequestBody MeetingRequestDTO.updateTitle request) {

        Long userId = SecurityUtil.getCurrentUserId();

        meetingCommandService.updateMeetingTitle(userId, Long.parseLong(meetingId), request.getTitle());

        return ApiResponse.onSuccess("제목수정이 완료되었습니다.");
    }

    @Operation(summary = "AI회의록 삭제", description =
            "# [v1.1 (2025-08-05)](https://www.notion.so/2265da7802c5800a97e4d66f8bf9626d)"+" meetingId를 받아 회의록을 삭제합니다. access token을 header에 입력해주세요."
    )
    @DeleteMapping("/{meetingId}")
    public ApiResponse<String> deleteMeeting(
            @PathVariable("meetingId") String meetingId) {

        Long userId = SecurityUtil.getCurrentUserId();

        meetingCommandService.deleteMeeting(userId, Long.parseLong(meetingId));

        return ApiResponse.onSuccess("회의가 삭제되었습니다.");
    }

    @Operation(summary = "AI회의록 단일조회", description =
            "# [v1.1 (2025-08-05)](https://www.notion.so/AI-2265da7802c580d1973ceb252bc9f1a5)"+" meetingId를 받아 회의내용을 조회합니다. access token을 header에 입력해주세요."
    )
    @GetMapping("/{meetingId}/ai-proceeding")
    public ApiResponse<MeetingResponseDTO.getMeetingProceeding> getMeetingProceeding(
        @PathVariable("meetingId")String meetingId) {

        Long userId = SecurityUtil.getCurrentUserId();
        MeetingResponseDTO.getMeetingProceeding response = meetingQueryService.getMeetingProceeding(userId, Long.parseLong(meetingId));

        return ApiResponse.onSuccess(response);
    }


    @Operation(summary = "AI회의록 proceeding 수정", description =
            "# [v1.1 (2025-08-05)](https://www.notion.so/AI-2265da7802c580e6b3aef0763bff0cf3)"+" meetingId와 수정된 Proceeding을 받아 회의록을 수정합니다. access token을 header에 입력해주세요."
    )
    @PatchMapping("/{meetingId}")
    public ApiResponse<String> adjustProceeding(
            @PathVariable("meetingId") String meetingId,
            @RequestBody MeetingRequestDTO.meetingProceedingRequest request) {

        Long userId = SecurityUtil.getCurrentUserId();

        meetingCommandService.adjustProceeding(userId, Long.parseLong(meetingId), request);

        return ApiResponse.onSuccess("회의가 수정되었습니다.");
    }

    @Operation(summary = "회의 종료", description =
            "# [v1.0 (2025-08-13)](https://www.notion.so/24e5da7802c5804f81b6e22f7b5106a1)" +
                    "회의 종료를 요청하는 API입니다. 회의가 종료되면 웹소켓 연결 해제, 회의 음성 파일 s3에 업로드, AI 회의록 생성이 순서대로 이루어집니다."
    )
    @PostMapping("/{meetingId}/end")
    public ApiResponse<String> endMeeting(
            @PathVariable("meetindId") String meetingId
    ) {
        Long userId = SecurityUtil.getCurrentUserId();

        meetingCommandService.endMeeting(userId, Long.parseLong(meetingId));

        return ApiResponse.onSuccess("회의가 종료되었습니다");
    }

    @Operation(summary = "회의록 다운로드", description =
            "# [v1.0 (2025-08-14)](https://www.notion.so/AI-2265da7802c580ba8447f248745cf9e7)" +
                    "회의록을 다운로드하는 API입니다. URL을 반환합니다."
    )
    @GetMapping("{meetingId}/ai-proceeding/download")
    public ApiResponse<MeetingResponseDTO.proceedingDownLoadLinkResponse> downloadMeeting(
            @PathVariable("meetingId") String meetingId
    ){
        Long userId = SecurityUtil.getCurrentUserId();

        MeetingResponseDTO.proceedingDownLoadLinkResponse response = meetingQueryService.downloadMeeting(userId, Long.parseLong(meetingId));

        return ApiResponse.onSuccess(response);
    }
}
