package com.haru.api.domain.meeting.service;

import com.haru.api.domain.meeting.converter.MeetingConverter;
import com.haru.api.domain.meeting.dto.MeetingResponseDTO;
import com.haru.api.domain.meeting.entity.Meeting;
import com.haru.api.domain.meeting.repository.MeetingRepository;
import com.haru.api.domain.snsEvent.entity.enums.Format;
import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.workspace.entity.Workspace;
import com.haru.api.global.annotation.TrackLastOpened;
import com.haru.api.global.apiPayload.code.status.ErrorStatus;
import com.haru.api.global.apiPayload.exception.handler.MeetingHandler;
import com.haru.api.infra.s3.AmazonS3Manager;
import com.haru.api.infra.api.entity.SpeechSegment;
import com.haru.api.infra.api.repository.SpeechSegmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingQueryServiceImpl implements MeetingQueryService{

    private final MeetingRepository meetingRepository;
    private final AmazonS3Manager amazonS3Manager;
    private final SpeechSegmentRepository speechSegmentRepository;

    @Override
    public List<MeetingResponseDTO.getMeetingResponse> getMeetings(User user, Workspace workspace) {

        List<Meeting> foundMeetings = meetingRepository.findByWorkspaceOrderByUpdatedAtDesc(workspace);

        return foundMeetings.stream()
                .map(eachMeeting -> MeetingConverter.toGetMeetingResponse(eachMeeting, user.getId()))
                .collect(Collectors.toList());
    }

    @Override
    @TrackLastOpened
    public MeetingResponseDTO.getMeetingProceeding getMeetingProceeding(User user, Meeting meeting){

        User foundMeetingCreator = meeting.getCreator();

        return MeetingConverter.toGetMeetingProceedingResponse(foundMeetingCreator, meeting);
    }

    @Override
    public MeetingResponseDTO.TranscriptResponse getTranscript(User user, Meeting meeting) {

        // Repository를 통해 SpeechSegment와 연관된 AIQuestion을 함께 조회 (N+1 문제 해결)
        List<SpeechSegment> segments = speechSegmentRepository.findAllByMeetingIdWithAIQuestions(meeting.getId());

        List<MeetingResponseDTO.Transcript> transcriptList = segments.stream()
                .map(MeetingResponseDTO.Transcript::from)
                .collect(Collectors.toList());

        return MeetingResponseDTO.TranscriptResponse.builder()
                .meetingStartTime(meeting.getStartTime())
                .transcripts(transcriptList)
                .build();
    }

    @Override
    public MeetingResponseDTO.proceedingDownLoadLinkResponse downloadMeeting(User user, Meeting meeting, Format format){
        String proceedingKeyName;
        switch (format) {
            case PDF:
                proceedingKeyName = meeting.getProceedingPdfKeyName();
                break;
            case DOCX:
                proceedingKeyName = meeting.getProceedingWordKeyName();
                break;
            default:
                throw new MeetingHandler(ErrorStatus.MEETING_INVALID_FILE_FORMAT);
        }

        if (proceedingKeyName == null || proceedingKeyName.isBlank()) {
            throw new MeetingHandler(ErrorStatus.MEETING_PROCEEDING_NOT_FOUND);
        }

        String presignedUrl = amazonS3Manager.generatePresignedUrl(proceedingKeyName);

        return MeetingResponseDTO.proceedingDownLoadLinkResponse.builder()
                .downloadLink(presignedUrl)
                .build();
    }

    @Override
    public MeetingResponseDTO.proceedingVoiceLinkResponse getMeetingVoiceFile(User user, Meeting meeting){

        String audioFileKeyName = meeting.getAudioFileKey();

        if (audioFileKeyName == null || audioFileKeyName.isBlank()) {
            throw new MeetingHandler(ErrorStatus.MEETING_PROCEEDING_NOT_FOUND);
        }

        String presignedUrl = amazonS3Manager.generatePresignedUrl(audioFileKeyName);

        return MeetingResponseDTO.proceedingVoiceLinkResponse.builder()
                .voiceLink(presignedUrl)
                .build();
    }

}
