package com.haru.api.domain.moodTracker.service;

import com.haru.api.domain.lastOpened.service.UserDocumentLastOpenedService;
import com.haru.api.domain.moodTracker.converter.MoodTrackerConverter;
import com.haru.api.domain.moodTracker.dto.MoodTrackerRequestDTO;
import com.haru.api.domain.moodTracker.dto.MoodTrackerResponseDTO;
import com.haru.api.domain.moodTracker.entity.*;
import com.haru.api.domain.moodTracker.entity.enums.MoodTrackerVisibility;
import com.haru.api.domain.moodTracker.repository.*;
import com.haru.api.domain.snsEvent.entity.enums.Format;
import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.userWorkspace.entity.UserWorkspace;
import com.haru.api.domain.userWorkspace.entity.enums.Auth;
import com.haru.api.domain.userWorkspace.repository.UserWorkspaceRepository;
import com.haru.api.domain.workspace.entity.Workspace;
import com.haru.api.domain.workspace.repository.WorkspaceRepository;
import com.haru.api.global.annotation.DeleteDocument;
import com.haru.api.global.annotation.UpdateDocumentTitle;
import com.haru.api.global.apiPayload.code.status.ErrorStatus;
import com.haru.api.global.apiPayload.exception.handler.*;
import com.haru.api.global.util.HashIdUtil;
import com.haru.api.infra.redis.RedisReportConsumer;
import com.haru.api.infra.redis.RedisReportProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.haru.api.domain.moodTracker.entity.enums.QuestionType.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MoodTrackerCommandServiceImpl implements MoodTrackerCommandService {

    private final MoodTrackerRepository moodTrackerRepository;
    private final UserWorkspaceRepository userWorkspaceRepository;

    private final SurveyQuestionRepository surveyQuestionRepository;
    private final MultipleChoiceRepository multipleChoiceRepository;
    private final CheckboxChoiceRepository checkboxChoiceRepository;

    private final MoodTrackerMailService moodTrackerMailService;

    private final MultipleChoiceAnswerRepository multipleChoiceAnswerRepository;
    private final CheckboxChoiceAnswerRepository checkboxChoiceAnswerRepository;
    private final SubjectiveAnswerRepository subjectiveAnswerRepository;

    private final MoodTrackerReportService moodTrackerReportService;
    private final RedisReportProducer redisReportProducer;
    private final RedisReportConsumer redisReportConsumer;

    private final HashIdUtil hashIdUtil;

    private final UserDocumentLastOpenedService userDocumentLastOpenedService;
    private final WorkspaceRepository workspaceRepository;

    /**
     * 분위기 트래커 생성
     */
    @Override
    @Transactional
    public MoodTrackerResponseDTO.CreateResult create(
            User user,
            Workspace workspace,
            MoodTrackerRequestDTO.CreateRequest request
    ) {

        Workspace foundWorkspace = workspaceRepository.findById(workspace.getId())
                .orElseThrow(() -> new WorkspaceHandler(ErrorStatus.WORKSPACE_NOT_FOUND));

        // 분위기 트래커 생성 및 저장
        MoodTracker moodTracker = MoodTrackerConverter.toMoodTracker(request, user, foundWorkspace);
        MoodTracker savedMoodTracker = moodTrackerRepository.save(moodTracker);

        // 선택지 생성 및 저장
        for (MoodTrackerRequestDTO.SurveyQuestion questionDTO : request.getQuestions()) {
            SurveyQuestion question = MoodTrackerConverter.toSurveyQuestion(questionDTO, moodTracker);
            surveyQuestionRepository.save(question);

            if (questionDTO.getType() == MULTIPLE_CHOICE) {
                List<MultipleChoice> choices = MoodTrackerConverter.toMultipleChoiceList(questionDTO.getOptions(), question);
                multipleChoiceRepository.saveAll(choices);
            } else if (questionDTO.getType() == CHECKBOX_CHOICE) {
                List<CheckboxChoice> choices = MoodTrackerConverter.toCheckboxChoiceList(questionDTO.getOptions(), question);
                checkboxChoiceRepository.saveAll(choices);
            }
        }

        // Redis Queue에 스케쥴링 추가
        redisReportProducer.scheduleReport(moodTracker.getId(), moodTracker.getDueDate());

        // mood tracker 생성 시 워크스페이스에 속해있는 모든 유저에 대해
        // last opened 테이블에 마지막으로 연 시간은 null로하여 추가
        List<User> usersInWorkspace = userWorkspaceRepository.findUsersByWorkspaceId(foundWorkspace.getId());
        userDocumentLastOpenedService.createInitialRecordsForWorkspaceUsers(usersInWorkspace, savedMoodTracker);

        return MoodTrackerConverter.toCreateResultDTO(moodTracker, hashIdUtil);
    }

    /**
     * 분위기 트래커 제목 수정
     */
    @Override
    @Transactional
    @UpdateDocumentTitle
    public void updateTitle(
            User user,
            MoodTracker moodTracker,
            MoodTrackerRequestDTO.UpdateTitleRequest request
    ) {
        UserWorkspace foundUserWorkspace = userWorkspaceRepository.findByWorkspaceIdAndUserId(moodTracker.getWorkspace().getId(), user.getId())
                .orElseThrow(() -> new UserWorkspaceHandler(ErrorStatus.USER_WORKSPACE_NOT_FOUND));

        // 워크스페이스 생성자이거나 해당 분위기 트래커 생성자인 경우 허용
        if (!(foundUserWorkspace.getAuth().equals(Auth.ADMIN)
                || moodTracker.getCreator().getId().equals(user.getId())))
            throw new MoodTrackerHandler(ErrorStatus.MOOD_TRACKER_MODIFY_NOT_ALLOWED);

        // 엔티티 업데이트
        moodTracker.updateTitle(request.getTitle());
        moodTrackerRepository.save(moodTracker);

        // 마감일 이후 && 썸네일이 생성된 시점이라면,
        if(moodTracker.getDueDate().isBefore(LocalDateTime.now()) && moodTracker.getThumbnailKey()!=null) {
            // 기존 썸네일 및 다운로드 파일 삭제
            moodTrackerReportService.deleteReportFileAndThumbnail(moodTracker.getId());
            // S3에서 썸네일 및 다운로드 파일 업데이트
            moodTrackerReportService.updateAndUploadReportFileAndThumbnail(moodTracker.getId());

        }
    }

    /**
     * 분위기 트래커 삭제
     */
    @Override
    @Transactional
    @DeleteDocument
    public void delete(
            User user,
            MoodTracker moodTracker
    ) {
        // redis queue에서 비워줘서 중복 처리 제외
        redisReportConsumer.removeFromQueue(moodTracker.getId());

        UserWorkspace foundUserWorkspace = userWorkspaceRepository.findByWorkspaceIdAndUserId(moodTracker.getWorkspace().getId(), user.getId())
                .orElseThrow(() -> new UserWorkspaceHandler(ErrorStatus.USER_WORKSPACE_NOT_FOUND));

        // 워크스페이스 생성자이거나 해당 분위기 트래커 생성자인 경우 허용
        if (!(foundUserWorkspace.getAuth().equals(Auth.ADMIN)
                || moodTracker.getCreator().getId().equals(user.getId())))
            throw new MoodTrackerHandler(ErrorStatus.MOOD_TRACKER_MODIFY_NOT_ALLOWED);

        // 마감일 이후 && 썸네일이 생성된 시점이라면,
        if(moodTracker.getDueDate().isBefore(LocalDateTime.now()) && moodTracker.getThumbnailKey()!=null) {
            // S3에서 썸네일 및 다운로드 파일 삭제
            moodTrackerReportService.deleteReportFileAndThumbnail(moodTracker.getId());
        }

        // 엔티티 삭제
        moodTrackerRepository.delete(moodTracker);
    }

    /**
     * 분위기 트래커 설문 링크 메일 전송
     */
    @Override
    @Transactional
    public void sendSurveyLink(
            MoodTracker moodTracker
    ) {

        String creatorName = moodTracker.getCreator().getName();  // 작성자 이름
        String surveyTitle = moodTracker.getTitle();              // 설문 제목

        String mailTitle = "%s 님이 나에게 [%s] 설문을 공유했습니다.".formatted(creatorName, surveyTitle);
        String mailContent = "%s 님의 [%s] 설문에 대한 소중한 의견을 보내주세요.".formatted(creatorName, surveyTitle);

        moodTrackerMailService.sendSurveyLinkToEmail(moodTracker.getId(), mailTitle, mailContent);
    }

    /**
     * 분위기 트래커 답변 제출
     */
    @Override
    @Transactional
    public void submitSurveyAnswers(
            Long moodTrackerId,
            MoodTrackerRequestDTO.SurveyAnswerList request
    ) {
        MoodTracker foundMoodTracker = moodTrackerRepository.findById(moodTrackerId)
                .orElseThrow(() -> new MoodTrackerHandler(ErrorStatus.MOOD_TRACKER_NOT_FOUND));

        // 마감일 이후이면 답변 불가능
        if(foundMoodTracker.getDueDate().isBefore(LocalDateTime.now())){
            throw new MoodTrackerHandler(ErrorStatus.MOOD_TRACKER_FINISHED);
        }

        List<SubjectiveAnswer> subjectiveAnswers = new ArrayList<>();
        List<MultipleChoiceAnswer> multipleChoiceAnswers = new ArrayList<>();
        List<CheckboxChoiceAnswer> checkboxChoiceAnswers = new ArrayList<>();

        // 전체 질문을 미리 조회 및 맵에 캐싱
        List<SurveyQuestion> foundQuestions = surveyQuestionRepository.findAllByMoodTrackerId(foundMoodTracker.getId());
        Map<Long, SurveyQuestion> questionMap = foundQuestions.stream()
                .collect(Collectors.toMap(SurveyQuestion::getId, q -> q));

        // 응답한 질문 ID 수집용
        Set<Long> answeredQuestionIds = new HashSet<>();

        for (MoodTrackerRequestDTO.SurveyAnswer dto : request.getAnswers()) {

            // 질문 엔티티 조회
            SurveyQuestion surveyQuestion = questionMap.get(dto.getQuestionId());
            if (surveyQuestion == null) {
                throw new MoodTrackerHandler(ErrorStatus.SURVEY_QUESTION_NOT_FOUND);
            }

            switch (dto.getType()) {
                case MULTIPLE_CHOICE -> {
                    // 질문 id와 선택지 id 함께 객관식 선택지 엔티티 조회 후 추가
                    MultipleChoice foundMultipleChoice = multipleChoiceRepository
                            .findByIdAndSurveyQuestionId(dto.getMultipleChoiceId(), dto.getQuestionId())
                            .orElseThrow(() -> new MoodTrackerHandler(ErrorStatus.INVALID_CHOICE_FOR_QUESTION));

                    multipleChoiceAnswers.add(
                            MoodTrackerConverter.toMultipleChoiceAnswer(foundMultipleChoice)
                    );
                }
                case CHECKBOX_CHOICE -> {
                    // 질문 id와 선택지 id 함께 체크박스 선택지 엔티티 목록 조회 후 추가
                    List<CheckboxChoice> foundCheckboxChoices = checkboxChoiceRepository
                            .findAllByIdInAndSurveyQuestionId(dto.getCheckboxChoiceIdList(), dto.getQuestionId());

                    // 요청 개수와 조회 개수가 다르면 → 유효하지 않은 선택지 포함
                    if (foundCheckboxChoices.size() != dto.getCheckboxChoiceIdList().size()) {
                        throw new MoodTrackerHandler(ErrorStatus.INVALID_CHOICE_FOR_QUESTION);
                    }

                    checkboxChoiceAnswers.addAll(
                            MoodTrackerConverter.toCheckboxChoiceAnswerList(foundCheckboxChoices)
                    );
                }
                case SUBJECTIVE -> {
                    // 주관식 답변 추가
                    subjectiveAnswers.add(
                            MoodTrackerConverter.toSubjectiveAnswer(surveyQuestion, dto.getSubjectiveAnswer())
                    );
                }
            }

            // 응답한 questionId 기록
            answeredQuestionIds.add(dto.getQuestionId());
        }

        // 필수 응답 누락 검사
        for (SurveyQuestion question : foundQuestions) {
            if (Boolean.TRUE.equals(question.getIsMandatory())
                    && !answeredQuestionIds.contains(question.getId())) {
                throw new MoodTrackerHandler(ErrorStatus.SURVEY_ANSWER_REQUIRED);
            }
        }

        // 일괄 저장
        multipleChoiceAnswerRepository.saveAll(multipleChoiceAnswers);
        checkboxChoiceAnswerRepository.saveAll(checkboxChoiceAnswers);
        subjectiveAnswerRepository.saveAll(subjectiveAnswers);

        // 답변자 수 증가
        moodTrackerRepository.addRespondentsNum(foundMoodTracker.getId());
    }

    @Override
    @Transactional
    public MoodTrackerResponseDTO.ReportDownLoadLinkResponse getDownloadLink(
            User user,
            MoodTracker moodTracker,
            Format format
    ) {

        // 권한 확인
        UserWorkspace userWorkspace = userWorkspaceRepository.findByWorkspaceIdAndUserId(
                moodTracker.getWorkspace().getId(), user.getId()
        ).orElseThrow(() -> new UserWorkspaceHandler(ErrorStatus.USER_WORKSPACE_NOT_FOUND));

        boolean hasAccess =
                userWorkspace.getAuth().equals(Auth.ADMIN) ||
                        moodTracker.getCreator().getId().equals(user.getId()) ||
                        moodTracker.getVisibility().equals(MoodTrackerVisibility.PUBLIC);

        if (!hasAccess) {
            throw new MoodTrackerHandler(ErrorStatus.MOOD_TRACKER_ACCESS_DENIED);
        }

        // 마감 여부 확인
        if (LocalDateTime.now().isBefore(moodTracker.getDueDate())) {
            throw new MoodTrackerHandler(ErrorStatus.MOOD_TRACKER_NOT_FINISHED);
        }

        String generatedReportUrl = moodTrackerReportService.generateDownloadLink(moodTracker, format);

        return MoodTrackerResponseDTO.ReportDownLoadLinkResponse.builder()
                .downloadLink(generatedReportUrl)
                .build();
    }

    @Override
    @Transactional
    public void generateReportTest(
            MoodTracker moodTracker
    ) {
        moodTrackerReportService.generateReport(moodTracker.getId());
    }

    @Override
    @Transactional
    public void generateReportFileAndThumbnailTest(
            MoodTracker moodTracker
    ) {
        // 중복 처리 제외
        redisReportConsumer.removeFromQueue(moodTracker.getId());

        // 즉시 생성
        moodTrackerReportService.generateAndUploadReportFileAndThumbnail(moodTracker.getId());
    }
}
