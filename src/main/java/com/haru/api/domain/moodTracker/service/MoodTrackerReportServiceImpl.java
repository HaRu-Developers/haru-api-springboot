package com.haru.api.domain.moodTracker.service;

import com.haru.api.domain.snsEvent.entity.enums.Format;
import com.haru.api.global.util.file.FileConvertHelper;
import com.haru.api.infra.api.dto.SurveyReportResponse;
import com.haru.api.domain.moodTracker.entity.*;
import com.haru.api.domain.moodTracker.repository.*;
import com.haru.api.global.apiPayload.code.status.ErrorStatus;
import com.haru.api.global.apiPayload.exception.handler.MoodTrackerHandler;
import com.haru.api.infra.api.client.ChatGPTClient;
import com.haru.api.infra.s3.AmazonS3Manager;
import com.haru.api.infra.s3.MarkdownFileUploader;
import com.lowagie.text.pdf.BaseFont;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import org.thymeleaf.context.Context;

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
    private final SpringTemplateEngine templateEngine;
    private final FileConvertHelper fileConvertHelper;

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
            Resource fontRes = new ClassPathResource("templates/NotoSansKR-Regular.ttf");
            if (!fontRes.exists()) {
                throw new IllegalStateException("Font not found: templates/NotoSansKR-Regular.ttf");
            }
            byte[] fontBytes;
            try (InputStream fin = fontRes.getInputStream()) {
                fontBytes = fin.readAllBytes();
            }

            // PDF: 제목 + 메타(작성자/마감일) + 본문 마크다운
            pdfReportBytes = createMoodTrackerPDFFromMarkdown(
                    foundMoodTracker.getTitle(),
                    foundMoodTracker.getCreator() != null ? foundMoodTracker.getCreator().getName() : null,
                    foundMoodTracker.getDueDate(),
                    foundMoodTracker.getReport(),
                    fontBytes
            );

            // DOCX: 제목 + 메타(작성자/마감일) + 본문 마크다운
            docxReportBytes = createMoodTrackerDocxFromMarkdown(
                    foundMoodTracker.getTitle(),
                    foundMoodTracker.getCreator() != null ? foundMoodTracker.getCreator().getName() : null,
                    foundMoodTracker.getDueDate(),
                    foundMoodTracker.getReport()
            );

        } catch (Exception e) {
            log.error("Error creating document", e);
            throw new MoodTrackerHandler(MOOD_TRACKER_DOWNLOAD_ERROR);
        }

        String fullPath = "mood-tracker/" + moodTrackerId;
        String pdfReportKey = amazonS3Manager.generateKeyName(fullPath) + ".pdf";
        String wordReportKey = amazonS3Manager.generateKeyName(fullPath) + ".docx";

        amazonS3Manager.uploadFile(pdfReportKey, pdfReportBytes, "application/pdf");
        amazonS3Manager.uploadFile(wordReportKey, docxReportBytes, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

        foundMoodTracker.updateReportKeyName(pdfReportKey, wordReportKey);

        String thumbnailKey = markdownFileUploader.createOrUpdateThumbnailWithPdfBytes(
                pdfReportBytes,
                "mood-tracker",
                null
        );
        foundMoodTracker.updateThumbnailKey(thumbnailKey);
    }

    /* ========================= 헬퍼들 ========================= */

    // PDF: 제목 + 메타(작성자/마감일) + 본문(마크다운) → 템플릿 렌더링 후 HTML→PDF 변환
    private byte[] createMoodTrackerPDFFromMarkdown(
            String title,
            String creator,
            Object dueDate,
            String markdown,
            byte[] fontBytes
    ) {
        try {
            // 템플릿 변수 구성
            Context ctx = new Context(java.util.Locale.KOREA);
            ctx.setVariable("title", (title == null || title.isBlank()) ? "팀 분위기 조사" : title);
            ctx.setVariable("creator", creator);
            ctx.setVariable("dueDate", fileConvertHelper.formatDueDate(dueDate));
            ctx.setVariable("reportHtml", fileConvertHelper.markdownToHtml(markdown));

            // 템플릿 렌더링
            String html = templateEngine.process("mood-tracker-report-template", ctx);

            // 스타일 주입
            html = fileConvertHelper.injectStyle(html);

            // HTML → PDF 변환 (OpenHTMLtoPDF 사용, 'NotoSansKR'로 등록)
            return fileConvertHelper.convertHtmlToPdf(html, fontBytes);
        } catch (Exception e) {
            log.error("createMoodTrackerPDFFromMarkdown failed", e);
            throw new MoodTrackerHandler(MOOD_TRACKER_DOWNLOAD_ERROR);
        }
    }

    // DOCX 생성: 제목 + 메타 + 마크다운 본문
    private byte[] createMoodTrackerDocxFromMarkdown(
            String title,
            String creator,
            Object dueDate,
            String markdown
    ) throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             XWPFDocument doc = new XWPFDocument()) {

            // 제목
            XWPFParagraph t = doc.createParagraph();
            t.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun tr = t.createRun();
            tr.setFontFamily("Noto Sans KR");
            tr.setText(title != null ? title : "Mood Tracker Report");
            tr.setBold(true);
            tr.setFontSize(22);
            tr.addBreak();

            // 메타(작성자 · 마감일)
            String metaCreator = (creator != null && !creator.isBlank()) ? ("작성자: " + creator) : null;
            String metaDue = fileConvertHelper.formatDueDate(dueDate);
            if (metaCreator != null || metaDue != null) {
                XWPFParagraph meta = doc.createParagraph();
                meta.setAlignment(ParagraphAlignment.CENTER);
                XWPFRun mr = meta.createRun();
                mr.setFontSize(12);
                StringBuilder sb = new StringBuilder();
                if (metaCreator != null) sb.append(metaCreator);
                if (metaCreator != null && metaDue != null) sb.append(" / ");
                if (metaDue != null) sb.append("마감일: ").append(metaDue);
                mr.setText(sb.toString());
                mr.setFontFamily("Noto Sans KR");
                mr.addBreak();
            }

            // 본문(마크다운 경량 파서)
            if (markdown == null) markdown = "";
            String[] lines = markdown.replace("\r\n", "\n").split("\n");

            for (String raw : lines) {
                String line = raw.trim();

                if (line.startsWith("### ")) {
                    // 시그니처 예) addHeading(XWPFDocument, String, int, String wordFontFamily)
                    fileConvertHelper.addHeading(doc, line.substring(4), 14, "Noto Sans KR");
                } else if (line.startsWith("## ")) {
                    fileConvertHelper.addHeading(doc, line.substring(3), 16, "Noto Sans KR");
                } else if (line.startsWith("# ")) {
                    fileConvertHelper.addHeading(doc, line.substring(2), 18, "Noto Sans KR");
                } else if (line.startsWith("- ")) {
                    // 예) addDocsBullet(XWPFDocument, String, String wordFontFamily)
                    fileConvertHelper.addDocsBullet(doc, line.substring(2), "Noto Sans KR");
                } else if (line.matches("^\\d+\\.\\s+.*")) {
                    fileConvertHelper.addDocsBullet(doc, line, "Noto Sans KR");
                } else {
                    // 예) addParagraph(XWPFDocument, String, int, boolean, String wordFontFamily)
                    fileConvertHelper.addParagraph(doc, line, 12, false, "Noto Sans KR");
                }
            }

            doc.write(out);
            return out.toByteArray();
        }
    }
}
