package com.haru.api.domain.meeting.service;

import com.haru.api.domain.lastOpened.repository.UserDocumentLastOpenedRepository;
import com.haru.api.domain.lastOpened.service.UserDocumentLastOpenedService;
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
import com.haru.api.infra.api.entity.SpeechSegment;
import com.haru.api.infra.api.repository.SpeechSegmentRepository;
import com.haru.api.infra.mp3encoder.Mp3EncoderService;
import com.haru.api.infra.s3.AmazonS3Manager;
import com.haru.api.infra.s3.MarkdownFileUploader;
import com.haru.api.infra.s3.MarkdownToPdfConverter;
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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
    private final UserDocumentLastOpenedService userDocumentLastOpenedService;
    private final WebSocketSessionRegistry webSocketSessionRegistry;
    private final SpeechSegmentRepository speechSegmentRepository;
    private final MarkdownToPdfConverter markdownToPdfConverter;
    private final MarkdownFileUploader markdownFileUploader;

    private final AmazonS3Manager amazonS3Manager;
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

        // meeting 생성 시 워크스페이스에 속해있는 모든 유저에 대해
        // last opened 테이블에 마지막으로 연 시간은 null로하여 추가
        List<User> usersInWorkspace = userWorkspaceRepository.findUsersByWorkspaceId(foundWorkspace.getId());
        userDocumentLastOpenedService.createInitialRecordsForWorkspaceUsers(usersInWorkspace, savedMeeting);

        return MeetingConverter.toCreateMeetingResponse(savedMeeting);
    }



    @Override
    @Transactional
    public void updateMeetingTitle(Long userId, Long meetingId, String newTitle) {


        Meeting foundMeeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new MeetingHandler(ErrorStatus.MEETING_NOT_FOUND));

        User foundUser = userRepository.findById(userId)
                .orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));

        // 회의 생성자 권한 확인
        if (!foundMeeting.getCreator().getId().equals(userId)) {
            throw new MemberHandler(ErrorStatus.MEMBER_NO_AUTHORITY);
        }

        foundMeeting.updateTitle(newTitle);

        // meeting 수정 시 워크스페이스에 속해있는 모든 유저에 대해
        // last opened 테이블에서 해당 문서 정보 업데이트
        userDocumentLastOpenedService.updateRecordsForWorkspaceUsers(foundMeeting);
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

        // meeting 삭제 시 워크스페이스에 속해있는 모든 유저에 대해
        // last opened 테이블에서 해당 문서 id를 가지고 있는 튜플 모두 삭제
        userDocumentLastOpenedService.deleteRecordsForWorkspaceUsers(foundMeeting);
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
    @Transactional
    public void processAfterMeeting(AudioSessionBuffer sessionBuffer) {

        // 현재 처리하고자 하는 session의 meeting entity
        Meeting currentMeeting = meetingRepository.findById(sessionBuffer.getMeeting().getId())
                .orElseThrow(() -> new MeetingHandler(ErrorStatus.MEETING_NOT_FOUND));

        // 버퍼에서 오디오 스트림 가져오기
        ByteArrayOutputStream audioBuffer = sessionBuffer.getAllBytes();

        if (audioBuffer != null && audioBuffer.size() > 0) {
            // 파일 업로드 후, key name을 반환
            String keyName = uploadAudioFile(audioBuffer);

            // 3. 조회한 엔티티의 상태를 변경합니다.
            currentMeeting.setAudioFileKey(keyName);

            // 4. AI 회의록 생성
            List<SpeechSegment> segments = speechSegmentRepository.findByMeeting(currentMeeting);

            if (segments.isEmpty()) {
                log.warn("meetingId: {}에 대한 대화 내용이 없어 AI 요약을 생략합니다.", currentMeeting.getId());
                return;
            }

            // 2. 모든 대화 텍스트를 하나의 문자열로 조합
            String documentText = segments.stream()
                    .map(SpeechSegment::getText)
                    .collect(Collectors.joining("\n"));

            String agendaResult = currentMeeting.getAgendaResult();

            // 동기적 분석 요청
            String analysisResult = chatGPTClient.analyzeMeetingTranscript(documentText, agendaResult).block();

            // 분석 결과 업데이트
            if (analysisResult != null && !analysisResult.isBlank()) {
                currentMeeting.updateProceeding(analysisResult);

                // --- PDF 및 썸네일 생성/업데이트 로직 시작 ---
                try {
                    // 생성된 PDF를 S3에 업로드
                    String pdfKey = markdownFileUploader.createOrUpdatePdf(analysisResult, "proceedings/", currentMeeting.getProceedingKeyName());
                    currentMeeting.initProceedingKeyName(pdfKey);

                    // 썸네일 생성 및 업데이트
                    String newThumbnailKey = markdownFileUploader.createOrUpdateThumbnail(pdfKey, "meetings/" + currentMeeting.getId(), currentMeeting.getThumbnailKey());
                    currentMeeting.initThumbnailKey(newThumbnailKey); // Meeting 엔티티에 썸네일 키 저장
                    log.info("회의록 썸네일 생성/업데이트 완료. Key: {}", newThumbnailKey);

                } catch (Exception e) {
                    log.error("meetingId: {}의 PDF 또는 썸네일 생성/업로드 중 에러 발생", currentMeeting.getId(), e);
                }
                log.info("meetingId: {}의 AI 회의록 생성 및 저장 완료.", currentMeeting.getId());
            } else {
                log.warn("meetingId: {}의 AI 분석 결과가 비어있습니다.", currentMeeting.getId());
            }



        } else {
            log.warn("meetingId: {}에 처리할 오디오 데이터가 없습니다.", currentMeeting.getId());
        }
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
            throw new MeetingHandler(ErrorStatus.MEETING_FILE_UPLOAD_FAIL);
        }
    }

    /**
     *
     * @param audioBuffer : 현재 처리하는 세션의 전체 원본 음성 데이터
     *
     * 전체 회의 음성 파일을 s3에 업로드하고, audio file key name을 저장합니다.
     */
    private String uploadAudioFile(ByteArrayOutputStream audioBuffer) {

        try {
            byte[] rawAudioData = audioBuffer.toByteArray();

            int channels = 1;
            int samplingRate = 16000;
            int bitRate = 128000;
            byte[] mp3Data = encoderService.encodePcmToMp3(rawAudioData, channels, samplingRate, bitRate);
            log.info("MP3 인코딩 완료. 인코딩된 크기: {} bytes", mp3Data.length);

            String keyName = amazonS3Manager.generateKeyName("meeting/recording") + ".mp3";
            amazonS3Manager.uploadFile(keyName, mp3Data, "audio/mpeg");
            log.info("S3 업로드 성공. Key: {}", keyName);

            return keyName;

        } catch (Exception e) {
            throw new MeetingHandler(ErrorStatus.MEETING_AUDIO_FILE_UPLOAD_FAIL);
        }
    }
}
