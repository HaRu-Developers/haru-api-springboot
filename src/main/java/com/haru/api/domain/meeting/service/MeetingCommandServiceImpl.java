package com.haru.api.domain.meeting.service;

import com.haru.api.domain.lastOpened.entity.UserDocumentId;
import com.haru.api.domain.lastOpened.entity.UserDocumentLastOpened;
import com.haru.api.domain.lastOpened.entity.enums.DocumentType;
import com.haru.api.domain.lastOpened.repository.UserDocumentLastOpenedRepository;
import com.haru.api.domain.meeting.converter.MeetingConverter;
import com.haru.api.domain.meeting.dto.MeetingRequestDTO;
import com.haru.api.domain.meeting.dto.MeetingResponseDTO;
import com.haru.api.domain.meeting.entity.Meeting;
import com.haru.api.domain.meeting.entity.Keyword;
import com.haru.api.domain.meeting.repository.MeetingRepository;
import com.haru.api.domain.meeting.repository.KeywordRepository;
import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.user.repository.UserRepository;
import com.haru.api.domain.userWorkspace.entity.UserWorkspace;
import com.haru.api.domain.userWorkspace.entity.enums.Auth;
import com.haru.api.domain.userWorkspace.repository.UserWorkspaceRepository;
import com.haru.api.domain.workspace.entity.Workspace;
import com.haru.api.domain.workspace.repository.WorkspaceRepository;
import com.haru.api.global.apiPayload.code.status.ErrorStatus;
import com.haru.api.global.apiPayload.exception.handler.*;
import com.haru.api.infra.api.client.ChatGPTClient;
import com.haru.api.infra.mp3encoder.Mp3EncoderService;
import com.haru.api.infra.s3.AmazonS3Manager;
import com.haru.api.infra.websocket.AudioSessionBuffer;
import com.haru.api.infra.websocket.WebSocketSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.docx4j.Docx4J;
import org.docx4j.TextUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.socket.CloseStatus;

import java.util.Optional;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingCommandServiceImpl implements MeetingCommandService {

    private final UserRepository userRepository;
    private final UserWorkspaceRepository userWorkspaceRepository;
    private final WorkspaceRepository workspaceRepository;
    private final MeetingRepository meetingRepository;
    private final KeywordRepository keywordRepository;
    private final ChatGPTClient chatGPTClient;
    private final UserDocumentLastOpenedRepository userDocumentLastOpenedRepository;
    private final WebSocketSessionRegistry webSocketSessionRegistry;

    private final AmazonS3Manager s3Manager;
    private final Mp3EncoderService encoderService;

    @Override
    @Transactional
    public MeetingResponseDTO.createMeetingResponse createMeeting(
            Long userId,
            MultipartFile agendaFile,
            MeetingRequestDTO.createMeetingRequest request)
    {
        User foundUser = userRepository.findById(userId)
                .orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));

        Workspace foundWorkspace = workspaceRepository.findById(request.getWorkspaceId())
                .orElseThrow(() -> new WorkspaceHandler(ErrorStatus.WORKSPACE_NOT_FOUND));

        String extractedText = extractTextFromFile(agendaFile);

        // agendaFile을 openAi 활용하여 요약
        String agendaResult = chatGPTClient.summarizeDocument(extractedText)
                .block();


        String agendaKeywords = "";
        String agendaSummary = "요약 생성에 실패했습니다.";

        if (agendaResult != null && agendaResult.contains("|||")) {
            String[] parts = agendaResult.split("\\|\\|\\|");
            if (parts.length == 2) {
                agendaKeywords = parts[0].trim();
                agendaSummary = parts[1].trim();
            } else {
                agendaSummary = agendaResult.trim();
            }
        }

        Meeting newMeeting = Meeting.createInitialMeeting(
                request.getTitle(),
                agendaSummary,
                foundUser,
                foundWorkspace
        );

        if (!agendaKeywords.isEmpty()) {
            String[] keywordsArray = agendaKeywords.split(",");
            for (String keyword : keywordsArray) {
                String trimmedKeyword = keyword.trim();
                if (trimmedKeyword.isEmpty()) continue;

                Keyword tag = keywordRepository.findByName(trimmedKeyword)
                        .orElseGet(() -> keywordRepository.save(Keyword.builder().name(trimmedKeyword).build()));

                newMeeting.addTag(tag);
            }
        }

        Meeting savedMeeting = meetingRepository.save(newMeeting);

        // meeting 생성 시 last opened에 추가
        // 마지막으로 연 시간은 null

        UserDocumentId documentId = new UserDocumentId(foundUser.getId(), savedMeeting.getId(), DocumentType.AI_MEETING_MANAGER);

        userDocumentLastOpenedRepository.save(
                UserDocumentLastOpened.builder()
                        .id(documentId)
                        .user(foundUser)
                        .title(savedMeeting.getTitle())
                        .workspaceId(foundWorkspace.getId())
                        .lastOpened(null)
                        .build()
        );

        return MeetingConverter.toCreateMeetingResponse(savedMeeting);
    }



    @Override
    @Transactional
    public void updateMeetingTitle(Long userId, Long meetingId, String newTitle) {


        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new MeetingHandler(ErrorStatus.MEETING_NOT_FOUND));

        User foundUser = userRepository.findById(userId)
                .orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));

        // 회의 생성자 권한 확인
        if (!meeting.getCreator().getId().equals(userId)) {
            throw new MemberHandler(ErrorStatus.MEMBER_NO_AUTHORITY);
        }

        meeting.updateTitle(newTitle);

        // last opened title 수정
        UserDocumentId userDocumentId = new UserDocumentId(userId, meetingId, DocumentType.AI_MEETING_MANAGER);

        UserDocumentLastOpened foundUserDocumentLastOpened = userDocumentLastOpenedRepository.findById(userDocumentId)
                .orElseThrow(() -> new UserDocumentLastOpenedHandler(ErrorStatus.USER_DOCUMENT_LAST_OPENED_NOT_FOUND));

        foundUserDocumentLastOpened.updateTitle(newTitle);
    }

    @Override
    @Transactional
    public void deleteMeeting(Long userId, Long meetingId) {
        User foundUser = userRepository.findById(userId)
                .orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));

        Meeting foundMeeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new MeetingHandler(ErrorStatus.MEETING_NOT_FOUND));

        Workspace foundWorkspace = meetingRepository.findWorkspaceByMeetingId(meetingId)
                .orElseThrow(() -> new WorkspaceHandler(ErrorStatus.WORKSPACE_NOT_FOUND));

        UserWorkspace foundUserWorkspace = userWorkspaceRepository.findByUserIdAndWorkspaceId(userId, foundWorkspace.getId())
                .orElseThrow(() -> new UserWorkspaceHandler(ErrorStatus.USER_WORKSPACE_NOT_FOUND));

        if (!foundMeeting.getCreator().getId().equals(userId) && !foundUserWorkspace.getAuth().equals(Auth.ADMIN)) {
            throw new MemberHandler(ErrorStatus.MEMBER_NO_AUTHORITY);
        }

        meetingRepository.delete(foundMeeting);

        // last opened 테이블 튜플 삭제
        // last opened가 없어도 오류 X
        UserDocumentId userDocumentId = new UserDocumentId(userId, meetingId, DocumentType.AI_MEETING_MANAGER);

        Optional<UserDocumentLastOpened> foundUserDocumentLastOpened = userDocumentLastOpenedRepository.findById(userDocumentId);

        foundUserDocumentLastOpened.ifPresent(userDocumentLastOpenedRepository::delete);
    }

    @Override
    @Transactional
    public void adjustProceeding(Long userId, Long meetingId, MeetingRequestDTO.meetingProceedingRequest newProceeding){
        User foundUser = userRepository.findById(userId)
                .orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));

        Meeting foundMeeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new MeetingHandler(ErrorStatus.MEETING_NOT_FOUND));

        Workspace foundWorkspace = meetingRepository.findWorkspaceByMeetingId(meetingId)
                .orElseThrow(() -> new WorkspaceHandler(ErrorStatus.WORKSPACE_NOT_FOUND));

        UserWorkspace foundUserWorkspace = userWorkspaceRepository.findByUserIdAndWorkspaceId(userId, foundWorkspace.getId())
                .orElseThrow(() -> new UserWorkspaceHandler(ErrorStatus.USER_WORKSPACE_NOT_FOUND));

        if (!foundMeeting.getCreator().getId().equals(userId) && !foundUserWorkspace.getAuth().equals(Auth.ADMIN)) {
            throw new MemberHandler(ErrorStatus.MEMBER_NO_AUTHORITY);
        }
        foundMeeting.updateProceeding(newProceeding.getProceeding());

    }

    @Override
    @Transactional
    public void endMeeting(Long userId, Long meetingId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));

        meetingRepository.findById(meetingId)
                .orElseThrow(() -> new MeetingHandler(ErrorStatus.MEETING_NOT_FOUND));

        Workspace foundWorkspace = meetingRepository.findWorkspaceByMeetingId(meetingId)
                .orElseThrow(() -> new WorkspaceHandler(ErrorStatus.WORKSPACE_NOT_FOUND));

        userWorkspaceRepository.findByUserIdAndWorkspaceId(userId, foundWorkspace.getId())
                .orElseThrow(() -> new UserWorkspaceHandler(ErrorStatus.USER_WORKSPACE_NOT_FOUND));

        // 웹소켓 연결 종료 및 세션 삭제
        try {
            webSocketSessionRegistry.getSession(meetingId).close(CloseStatus.BAD_DATA.withReason("Invalid path"));
            webSocketSessionRegistry.removeSession(meetingId);
        } catch (Exception e) {
            log.error("meetingId: {} session 종료 오류", meetingId);
        }
    }

    @Override
    @Async
    public void processAfterMeeting(AudioSessionBuffer sessionBuffer) {

        // 현재 처리하고자 하는 session의 meeting entity
        Meeting currentMeeting = sessionBuffer.getMeeting();

        // 해당 세션의 전체 오디오 버퍼 가져오기
        ByteArrayOutputStream audioBuffer = sessionBuffer.getAllBytes();

        // 음성 파일 s3에 업로드
        uploadAudioFile(audioBuffer, currentMeeting);

        // todo: chatGPT를 사용하여 AI 회의록 생성하는 기능

    }

    private List<String> convertFileToImages(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Collections.emptyList();
        }

        String filename = file.getOriginalFilename();
        try {
            if (filename != null && filename.toLowerCase().endsWith(".pdf")) {
                return convertPdfToImages(file.getInputStream());
            } else if (filename != null && filename.toLowerCase().endsWith(".docx")) {
                return convertDocxToImages(file.getInputStream());
            } else {
                return Collections.emptyList();
            }
        } catch (Exception e) {
            throw new RuntimeException("파일을 이미지로 변환하는 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * PDF 스트림을 이미지(Base64) 리스트로 변환
     */
    private List<String> convertPdfToImages(InputStream inputStream) throws IOException {
        List<String> base64Images = new ArrayList<>();
        try (PDDocument document = PDDocument.load(inputStream)) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI(i, 300);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, "png", baos);
                base64Images.add(Base64.getEncoder().encodeToString(baos.toByteArray()));
            }
        }
        return base64Images;
    }

    /**
     * DOCX 스트림을 이미지(Base64) 리스트로 변환 (내부적으로 PDF로 변환 후 처리)
     * docx의 폰트들을 서버에 다운로드해야지 사용가능 (CI) - 현재 불가능
     */
    private List<String> convertDocxToImages(InputStream inputStream) throws Exception {
        // docx -> pdf 변환
        WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.load(inputStream);
        ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream();
        Docx4J.toPDF(wordMLPackage, pdfOutputStream);

        return convertPdfToImages(new ByteArrayInputStream(pdfOutputStream.toByteArray()));
    }

    /**
     * MultipartFile을 받아 파일 형식에 따라 텍스트를 추출합니다.
     */
    private String extractTextFromFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "";
        }

        String filename = file.getOriginalFilename();
        try (InputStream inputStream = file.getInputStream()) {
            if (filename != null && filename.toLowerCase().endsWith(".pdf")) {
                // PDF에서 텍스트 추출
                try (PDDocument document = PDDocument.load(inputStream)) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    return stripper.getText(document);
                }
            } else if (filename != null && filename.toLowerCase().endsWith(".docx")) {
                // DOCX에서 텍스트 추출
                WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.load(inputStream);
                // StringWriter를 사용하여 문서의 모든 텍스트 파트를 더 안정적으로 추출합니다.
                StringWriter stringWriter = new StringWriter();
                TextUtils.extractText(wordMLPackage.getMainDocumentPart(), stringWriter);
                return stringWriter.toString();
            } else {
                log.warn("지원하지 않는 파일 형식입니다: {}", filename);
                return "";
            }
        } catch (Exception e) {
            log.error("파일에서 텍스트를 추출하는 중 오류가 발생했습니다.", e);
            throw new RuntimeException("파일 텍스트 추출에 실패했습니다.", e);
        }
    }

    /**
     *
     * @param audioBuffer : 현재 처리하는 세션의 전체 원본 음성 데이터
     * @param currentMeeting : 현재 처리하는 meeting
     *
     * 전체 회의 음성 파일을 s3에 업로드하고, audio file key name을 저장합니다.
     */
    private void uploadAudioFile(ByteArrayOutputStream audioBuffer, Meeting currentMeeting) {

        // 버퍼에 데이터가 있는지 확인
        if (audioBuffer != null && audioBuffer.size() > 0) {
            try{
                // 원본 오디오 데이터를 byte[]로 변환
                byte[] rawAudioData = audioBuffer.toByteArray();

                // 클라이언트 오디오 설정에 맞춰 MP3로 인코딩
                int channels = 1;
                int samplingRate = 16000; // 16kHz
                int bitRate = 128000;     // 128kbps
                byte[] mp3Data = encoderService.encodePcmToMp3(rawAudioData, channels, samplingRate, bitRate);
                log.info("MP3 인코딩 완료. 인코딩된 크기: {} bytes", mp3Data.length);

                // S3에 저장할 고유한 키를 생성 및 저장 (확장자 .mp3 추가)
                String keyName = s3Manager.generateKeyName("meeting/recording") + ".mp3";

                // S3에 인코딩된 MP3 파일을 업로드
                s3Manager.uploadFile(keyName, mp3Data, "audio/mpeg"); // MP3의 MIME 타입은 "audio/mpeg"
                log.info("S3 업로드 성공. Key: {}", keyName);

                // meeting entity에 audio key name 저장
                currentMeeting.setAudioFileKey(keyName);
                meetingRepository.save(currentMeeting);

                // WebSocketSessionRegistry에서 해당 session 제거
                webSocketSessionRegistry.removeSession(currentMeeting.getId());
            } catch (Exception e) {
                throw new MeetingHandler(ErrorStatus.MEETING_AUDIO_FILE_UPLOAD_FAIL);
            }
        } else {
            log.warn("meetingId: {}에 처리할 오디오 데이터가 없습니다.", currentMeeting.getId());
        }
    }
}
