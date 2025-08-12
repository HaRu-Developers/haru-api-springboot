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
import com.haru.api.domain.meeting.repository.MeetingKeywordRepository;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.docx4j.Docx4J;
import org.docx4j.TextUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
                        .title(savedMeeting.getTitle())
                        .workspaceId(foundWorkspace.getId())
                        .lastOpened(null)
                        .build()
        );

        return MeetingConverter.toCreateMeetingResponse(savedMeeting);
    }



    @Override
    @Transactional
    public void updateMeetingTitle(Long userId, String meetingId, String newTitle) {

        Long foundMeetingId = Long.parseLong(meetingId);

        Meeting meeting = meetingRepository.findById(foundMeetingId)
                .orElseThrow(() -> new MeetingHandler(ErrorStatus.MEETING_NOT_FOUND));

        User foundUser = userRepository.findById(userId)
                .orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));

        // 회의 생성자 권한 확인
        if (!meeting.getCreator().getId().equals(userId)) {
            throw new MemberHandler(ErrorStatus.MEMBER_NO_AUTHORITY);
        }

        meeting.updateTitle(newTitle);

        // last opened title 수정
        UserDocumentId userDocumentId = new UserDocumentId(userId, foundMeetingId, DocumentType.AI_MEETING_MANAGER);

        UserDocumentLastOpened foundUserDocumentLastOpened = userDocumentLastOpenedRepository.findById(userDocumentId)
                .orElseThrow(() -> new UserDocumentLastOpenedHandler(ErrorStatus.USER_DOCUMENT_LAST_OPENED_NOT_FOUND));

        foundUserDocumentLastOpened.updateTitle(newTitle);
    }

    @Override
    @Transactional
    public void deleteMeeting(Long userId, String meetingId) {
        User foundUser = userRepository.findById(userId)
                .orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));

        Long foundMeetingId = Long.parseLong(meetingId);

        Meeting foundMeeting = meetingRepository.findById(foundMeetingId)
                .orElseThrow(() -> new MeetingHandler(ErrorStatus.MEETING_NOT_FOUND));

        Workspace foundWorkspace = meetingRepository.findWorkspaceByMeetingId(foundMeetingId)
                .orElseThrow(() -> new WorkspaceHandler(ErrorStatus.WORKSPACE_NOT_FOUND));

        UserWorkspace foundUserWorkspace = userWorkspaceRepository.findByUserIdAndWorkspaceId(userId, foundWorkspace.getId())
                .orElseThrow(() -> new UserWorkspaceHandler(ErrorStatus.USER_WORKSPACE_NOT_FOUND));

        if (!foundMeeting.getCreator().getId().equals(userId) && !foundUserWorkspace.getAuth().equals(Auth.ADMIN)) {
            throw new MemberHandler(ErrorStatus.MEMBER_NO_AUTHORITY);
        }

        meetingRepository.delete(foundMeeting);

        // last opened 테이블 튜플 삭제
        // last opened가 없어도 오류 X
        UserDocumentId userDocumentId = new UserDocumentId(userId, foundMeetingId, DocumentType.AI_MEETING_MANAGER);

        Optional<UserDocumentLastOpened> foundUserDocumentLastOpened = userDocumentLastOpenedRepository.findById(userDocumentId);

        foundUserDocumentLastOpened.ifPresent(userDocumentLastOpenedRepository::delete);
    }

    @Override
    @Transactional
    public void adjustProceeding(Long userId, String meetingId, MeetingRequestDTO.meetingProceedingRequest newProceeding){
        User foundUser = userRepository.findById(userId)
                .orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));

        Long foundMeetingId = Long.parseLong(meetingId);

        Meeting foundMeeting = meetingRepository.findById(foundMeetingId)
                .orElseThrow(() -> new MeetingHandler(ErrorStatus.MEETING_NOT_FOUND));

        Workspace foundWorkspace = meetingRepository.findWorkspaceByMeetingId(foundMeetingId)
                .orElseThrow(() -> new WorkspaceHandler(ErrorStatus.WORKSPACE_NOT_FOUND));

        UserWorkspace foundUserWorkspace = userWorkspaceRepository.findByUserIdAndWorkspaceId(userId, foundWorkspace.getId())
                .orElseThrow(() -> new UserWorkspaceHandler(ErrorStatus.USER_WORKSPACE_NOT_FOUND));

        if (!foundMeeting.getCreator().getId().equals(userId) && !foundUserWorkspace.getAuth().equals(Auth.ADMIN)) {
            throw new MemberHandler(ErrorStatus.MEMBER_NO_AUTHORITY);
        }
        foundMeeting.updateProceeding(newProceeding.getProceeding());

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
}
