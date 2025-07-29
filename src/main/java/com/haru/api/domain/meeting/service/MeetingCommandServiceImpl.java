package com.haru.api.domain.meeting.service;

import com.haru.api.domain.meeting.converter.MeetingConverter;
import com.haru.api.domain.meeting.dto.MeetingRequestDTO;
import com.haru.api.domain.meeting.dto.MeetingResponseDTO;
import com.haru.api.domain.meeting.entity.Meeting;
import com.haru.api.domain.meeting.repository.MeetingRepository;
import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.user.repository.UserRepository;
import com.haru.api.domain.workspace.entity.Workspace;
import com.haru.api.domain.workspace.repository.WorkspaceRepository;
import com.haru.api.global.apiPayload.code.status.ErrorStatus;
import com.haru.api.global.apiPayload.exception.handler.MeetingHandler;
import com.haru.api.global.apiPayload.exception.handler.MemberHandler;
import com.haru.api.global.apiPayload.exception.handler.WorkspaceHandler;
import com.haru.api.infra.api.client.ChatGPTClient;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.docx4j.Docx4J;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingCommandServiceImpl implements MeetingCommandService {

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final MeetingRepository meetingRepository;
    private final ChatGPTClient chatGPTClient;

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



        List<String> images = convertFileToImages(agendaFile);






        // agendaFile을 openAi 활용하여 요약 - 미구현
        String agendaResult = chatGPTClient.summarizePdf(agendaFile)
                .block();


        Meeting newMeeting = Meeting.createInitialMeeting(
                request.getTitle(),
                agendaResult,
                foundUser,
                foundWorkspace
        );

        Meeting savedMeeting = meetingRepository.save(newMeeting);


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

    }

    @Override
    @Transactional
    public void deleteMeeting(Long userId, Long meetingId) {
        User foundUser = userRepository.findById(userId)
                .orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new MeetingHandler(ErrorStatus.MEETING_NOT_FOUND));

        // 삭제권한 확인
        if (!meeting.getCreator().getId().equals(userId)) {
            throw new MemberHandler(ErrorStatus.MEMBER_NO_AUTHORITY);
        }

        meetingRepository.delete(meeting);
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
     */
    private List<String> convertDocxToImages(InputStream inputStream) throws Exception {
        // docx -> pdf 변환
        WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.load(inputStream);
        ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream();
        Docx4J.toPDF(wordMLPackage, pdfOutputStream);

        return convertPdfToImages(new ByteArrayInputStream(pdfOutputStream.toByteArray()));
    }
}
