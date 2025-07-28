package com.haru.api.domain.moodTracker.service;

import com.haru.api.infra.api.dto.SurveyReportResponse;
import com.haru.api.domain.moodTracker.entity.*;
import com.haru.api.domain.moodTracker.repository.*;
import com.haru.api.global.apiPayload.code.status.ErrorStatus;
import com.haru.api.global.apiPayload.exception.handler.MoodTrackerHandler;
import com.haru.api.infra.api.client.ChatGPTClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

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
}
