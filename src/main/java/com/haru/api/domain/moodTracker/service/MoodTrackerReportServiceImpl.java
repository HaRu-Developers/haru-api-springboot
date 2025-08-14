package com.haru.api.domain.moodTracker.service;

import com.haru.api.domain.snsEvent.entity.enums.Format;
import com.haru.api.infra.api.dto.SurveyReportResponse;
import com.haru.api.domain.moodTracker.entity.*;
import com.haru.api.domain.moodTracker.repository.*;
import com.haru.api.global.apiPayload.code.status.ErrorStatus;
import com.haru.api.global.apiPayload.exception.handler.MoodTrackerHandler;
import com.haru.api.infra.api.client.ChatGPTClient;
import com.haru.api.infra.s3.AmazonS3Manager;
import com.haru.api.infra.s3.MarkdownFileUploader;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static com.haru.api.global.apiPayload.code.status.ErrorStatus.*;
import static com.haru.api.global.apiPayload.code.status.ErrorStatus.MOOD_TRACKER_DOWNLOAD_ERROR;

@Service
@RequiredArgsConstructor
@Slf4j
public class MoodTrackerReportServiceImpl implements MoodTrackerReportService {

    private final ChatGPTClient chatGPTClient;
    private final MoodTrackerRepository moodTrackerRepository;
    private final SurveyQuestionRepository surveyQuestionRepository;
    private final SubjectiveAnswerRepository subjectiveAnswerRepository;
    private final MultipleChoiceAnswerRepository multipleChoiceAnswerRepository;
    private final CheckboxChoiceAnswerRepository checkboxChoiceAnswerRepository;

    private final AmazonS3Manager amazonS3Manager;
    private final MarkdownFileUploader markdownFileUploader;

    @Async
    public void generateReport(Long moodTrackerId) {
        MoodTracker foundMoodTracker = moodTrackerRepository.findById(moodTrackerId)
                .orElseThrow(() -> new MoodTrackerHandler(ErrorStatus.MOOD_TRACKER_NOT_FOUND));

        // 전체 질문 조회 (ID 기준 정렬 보장)
        List<SurveyQuestion> questions = surveyQuestionRepository.findAllByMoodTrackerId(moodTrackerId);

        // 응답 수집 (질문 기반 조회)
        List<SubjectiveAnswer> subjectiveAnswerList = subjectiveAnswerRepository.findAllBySurveyQuestionIn(questions);
        List<MultipleChoiceAnswer> multipleAnswerList = multipleChoiceAnswerRepository.findAllByMultipleChoice_SurveyQuestionIn(questions);
        List<CheckboxChoiceAnswer> checkboxAnswerList = checkboxChoiceAnswerRepository.findAllByCheckboxChoice_SurveyQuestionIn(questions);

        // 통계 생성용 맵
        Map<Long, List<SubjectiveAnswer>> subjectiveMap = subjectiveAnswerList.stream()
                .collect(Collectors.groupingBy(ans -> ans.getSurveyQuestion().getId()));

        Map<Long, Map<String, Long>> multipleStats = new HashMap<>();
        for (MultipleChoiceAnswer multipleChoiceAnswer : multipleAnswerList) {
            Long qid = multipleChoiceAnswer.getMultipleChoice().getSurveyQuestion().getId();
            String content = multipleChoiceAnswer.getMultipleChoice().getContent();
            multipleStats.computeIfAbsent(qid, k -> new LinkedHashMap<>());
            multipleStats.get(qid).merge(content, 1L, Long::sum);
        }

        Map<Long, Map<String, Long>> checkboxStats = new HashMap<>();
        for (CheckboxChoiceAnswer ans : checkboxAnswerList) {
            Long qid = ans.getCheckboxChoice().getSurveyQuestion().getId();
            String content = ans.getCheckboxChoice().getContent();
            checkboxStats.computeIfAbsent(qid, k -> new LinkedHashMap<>());
            checkboxStats.get(qid).merge(content, 1L, Long::sum);
        }

        // 프롬프트 생성
        String prompt = buildPrompt(foundMoodTracker.getTitle(), questions, subjectiveMap, multipleStats, checkboxStats);

        try {
            // GPT 호출 + 파싱
            SurveyReportResponse response = chatGPTClient.getMoodTrackerReport(prompt).block();
            log.debug("[GPT 파싱 성공]\n{}\n{}", response.getReport(), response.getSuggestionsByQuestionId());

            // 전체 리포트 저장
            foundMoodTracker.createReport(response.getReport());

            // 제안 저장
            Map<Long, String> suggestionMap = response.getSuggestionsByQuestionId();
            for (SurveyQuestion question : questions) {
                Long qid = question.getId();
                if (suggestionMap.containsKey(qid)) {
                    String suggestion = suggestionMap.get(qid);
                    if (suggestion != null && !suggestion.isBlank()) {
                        log.debug("[Suggestion 저장]\n{}: {}", qid, suggestion);
                        question.createSuggestion(suggestion);
                    }
                }
            }

        } catch (IllegalStateException e) {
            log.warn("이미 suggestion이 생성된 질문이 존재합니다. 일부 항목은 건너뜁니다.");
        } catch (Exception e) {
            throw new RuntimeException("GPT 응답 파싱 실패", e);
        }

        moodTrackerRepository.save(foundMoodTracker);


    }

    private String buildPrompt(String title,
                               List<SurveyQuestion> questions,
                               Map<Long, List<SubjectiveAnswer>> subjectiveMap,
                               Map<Long, Map<String, Long>> multipleStats,
                               Map<Long, Map<String, Long>> checkboxStats) {

        StringBuilder sb = new StringBuilder();

        sb.append("아래는 설문 문항입니다. 각 문항에는 객관식, 체크박스, 주관식 응답이 섞여 있으며, 무조건 활용하세요.\n");

        sb.append("다음은 '").append(title).append("' 설문에 대한 객관식 답변 통계 및 주관식 답변입니다.\n\n");

        for (SurveyQuestion question : questions) {
            Long qid = question.getId();
            sb.append("질문 id: ").append(qid).append("\n");
            sb.append("질문 내용: ").append(question.getTitle()).append("\n");

            switch (question.getType()) {
                case SUBJECTIVE -> {
                    List<SubjectiveAnswer> answers = subjectiveMap.getOrDefault(qid, List.of());
                    if (answers.isEmpty()) {
                        sb.append("- (응답 없음)\n");
                    } else {
                        for (SubjectiveAnswer ans : answers) {
                            sb.append("- ").append(ans.getAnswer()).append("\n");
                        }
                    }
                }

                case MULTIPLE_CHOICE -> {
                    Map<String, Long> stat = multipleStats.getOrDefault(qid, Map.of());
                    List<MultipleChoice> choices = question.getMultipleChoiceList();
                    for (MultipleChoice choice : choices) {
                        String content = choice.getContent();
                        Long count = stat.getOrDefault(content, 0L);
                        sb.append("- ").append(content).append(": ").append(count).append("명\n");
                    }
                }

                case CHECKBOX_CHOICE -> {
                    Map<String, Long> stat = checkboxStats.getOrDefault(qid, Map.of());
                    List<CheckboxChoice> choices = question.getCheckboxChoiceList();
                    for (CheckboxChoice choice : choices) {
                        String content = choice.getContent();
                        Long count = stat.getOrDefault(content, 0L);
                        sb.append("- ").append(content).append(": ").append(count).append("명\n");
                    }
                }
            }

            sb.append("\n");
        }

        return sb.toString();
    }

    @Override
    public String generateDownloadLink(
            MoodTracker moodTracker,
            Format format
    ) {
        String downloadLink = "";

        String moodTrackerTitle = moodTracker.getTitle();

        if (format == Format.PDF) {
            String keyName = moodTracker.getPdfReportKey();
            if (keyName == null || keyName.isEmpty()) {
                throw new MoodTrackerHandler(MOOD_TRACKER_KEYNAME_NOT_FOUND);
            }
            downloadLink = amazonS3Manager.generatePresignedUrlForDownloadPdfAndWord(keyName, moodTrackerTitle + "_리포트.pdf");
        } else if (format == Format.DOCX) {
            String keyName = moodTracker.getWordReportKey();
            if (keyName == null || keyName.isEmpty()) {
                throw new MoodTrackerHandler(MOOD_TRACKER_KEYNAME_NOT_FOUND);
            }
            downloadLink = amazonS3Manager.generatePresignedUrlForDownloadPdfAndWord(keyName, moodTrackerTitle + "_리포트.docx");
        } else {
            throw new MoodTrackerHandler(MOOD_TRACKER_WRONG_FORMAT);
        }

        return downloadLink;
    }

    @Override
    public void generateAndUploadReportFileAndThumbnail(Long moodTrackerId){

        // 리포트 생성
        generateReport(moodTrackerId);

        MoodTracker foundMoodTracker = moodTrackerRepository.findById(moodTrackerId)
                .orElseThrow(() -> new MoodTrackerHandler(ErrorStatus.MOOD_TRACKER_NOT_FOUND));

        byte[] pdfReportBytes;
        byte[] docxReportBytes;
        try {
            // 폰트 경로
            URL resource = getClass().getClassLoader().getResource("templates/NotoSansKR-Regular.ttf");
            File reg = new File(resource.toURI());
            try (ByteArrayOutputStream pdfOut = new ByteArrayOutputStream()) {
                Document document = new Document();
                PdfWriter.getInstance(document, pdfOut);
                document.open();

                // 한글 폰트 지정
                BaseFont baseFont = BaseFont.createFont(reg.getAbsolutePath(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                Font font = new Font(baseFont, 12);

                document.add(new Paragraph("Mood Tracker Report", font));
                document.add(new Paragraph("제목: " + foundMoodTracker.getTitle(), font));
                document.add(new Paragraph("작성자: " + foundMoodTracker.getCreator().getName(), font));
                document.add(new Paragraph("마감일: " + foundMoodTracker.getDueDate(), font));
                document.add(new Paragraph("리포트 내용: " + foundMoodTracker.getReport(), font));

                document.close();
                pdfReportBytes = pdfOut.toByteArray();
            }

            // ====== DOCX 생성 (Apache POI) ======
            try (ByteArrayOutputStream docxOut = new ByteArrayOutputStream()) {
                XWPFDocument doc = new XWPFDocument();

                // 제목
                XWPFParagraph titlePara = doc.createParagraph();
                XWPFRun titleRun = titlePara.createRun();
                titleRun.setText("Mood Tracker Report");
                titleRun.setBold(true);
                titleRun.setFontFamily("Noto Sans KR");
                titleRun.setFontSize(14);

                // 내용
                XWPFParagraph contentPara = doc.createParagraph();
                XWPFRun contentRun = contentPara.createRun();
                contentRun.setText("제목: " + foundMoodTracker.getTitle());
                contentRun.addBreak();
                contentRun.setText("작성자: " + foundMoodTracker.getCreator().getName());
                contentRun.addBreak();
                contentRun.setText("마감일: " + foundMoodTracker.getDueDate());
                contentRun.addBreak();
                contentRun.setText("리포트 내용: " + foundMoodTracker.getReport());

                doc.write(docxOut);
                docxReportBytes = docxOut.toByteArray();
            }

        } catch (Exception e) {
            log.error("Error creating document: {}", e.getMessage());
            throw new MoodTrackerHandler(MOOD_TRACKER_DOWNLOAD_ERROR);
        }
        // PDF, DOCS파일, 썸네일 S3에 업로드 및 DB에 keyName저장
        String fullPath = "mood-tracker/" + moodTrackerId;
        String pdfReportKey = amazonS3Manager.generateKeyName(fullPath) + "." + "pdf";
        String wordReportKey = amazonS3Manager.generateKeyName(fullPath) + "." + "docx";
        amazonS3Manager.uploadFile(pdfReportKey, pdfReportBytes, "application/pdf");
        amazonS3Manager.uploadFile(wordReportKey, docxReportBytes, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        // 분위기 트래커에 keyName 저장
        foundMoodTracker.updateReportKeyName(
                pdfReportKey,
                wordReportKey
        );
        // 분위기 트래커 리포트의 PDF 첫페이지를 썸네일로 저장
        String thumbnailKey = markdownFileUploader.createOrUpdateThumbnailWithPdfBytes(
                pdfReportBytes,
                "mood-tracker",
                null
        );
        foundMoodTracker.updateThumbnailKey(thumbnailKey);
    }

    // 파일명 인코딩
    private String buildEncodedContentDisposition(String originalFilename) {
        String encodedFilename = URLEncoder.encode(originalFilename, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20"); // 공백 처리
        return "attachment; filename*=UTF-8''" + encodedFilename;
    }
}
