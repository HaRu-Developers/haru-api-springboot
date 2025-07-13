package com.haru.api.domain.meeting.controller;

import com.haru.api.domain.meeting.dto.MeetingRequestDTO;
import com.haru.api.domain.meeting.dto.MeetingResponseDTO;
import com.haru.api.domain.meeting.service.MeetingService;
import com.haru.api.domain.user.security.jwt.SecurityUtil;
import com.haru.api.global.apiPayload.ApiResponse;
import com.haru.api.global.apiPayload.code.status.ErrorStatus;
import com.haru.api.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/meetings")
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;

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

        MeetingResponseDTO.createMeetingResponse response = meetingService.createMeeting(userId, agendaFile, request);

        return ApiResponse.onSuccess(response);
    }
}
