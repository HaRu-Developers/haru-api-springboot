package com.haru.api.domain.moodTracker.service;

import com.haru.api.domain.moodTracker.entity.MoodTracker;
import com.haru.api.domain.moodTracker.repository.MoodTrackerRepository;
import com.haru.api.domain.userWorkspace.repository.UserWorkspaceRepository;
import com.haru.api.global.apiPayload.code.status.ErrorStatus;
import com.haru.api.global.apiPayload.exception.handler.MoodTrackerHandler;
import com.haru.api.global.util.HashIdUtil;
import com.haru.api.infra.mail.EmailSender;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MoodTrackerMailServiceImpl implements MoodTrackerMailService {

    private final UserWorkspaceRepository userWorkspaceRepository;
    private final MoodTrackerRepository moodTrackerRepository;
    private final EmailSender emailSender;

    private final HashIdUtil hashIdUtil;

    @Value("${survey-url}")
    private String surveyBaseUrl;

    @Override
    public void sendSurveyLinkToEmail(
            Long moodTrackerId,
            String mailTitle,
            String mailContent
    ) {
        MoodTracker foundMoodTracker = moodTrackerRepository.findById(moodTrackerId)
                .orElseThrow(() -> new MoodTrackerHandler(ErrorStatus.MOOD_TRACKER_NOT_FOUND));

        // id hash 처리
        String surveyLink = surveyBaseUrl + "/" + hashIdUtil.encode(moodTrackerId);

        Long workspaceId = foundMoodTracker.getWorkspace().getId();
        List<String> foundEmails = userWorkspaceRepository.findEmailsByWorkspaceId(workspaceId);

        for (String email : foundEmails) {
            String htmlContent = buildHtmlEmail(mailContent, surveyLink);
            emailSender.send(email, mailTitle, htmlContent);
        }
    }

    private String buildHtmlEmail(String description, String link) {
        return String.format(
                "<html>" +
                        "<head></head>" +
                        "<body style=\"font-family: sans-serif;\">" +
                        "  <p>안녕하세요,</p>" +
                        "  <p>%s</p>" +
                        "  <p>아래 버튼을 클릭하여 설문에 참여해 주세요!</p>" +
                        "  <p style=\"margin-top: 20px;\">" +
                        "    <a href=\"%s\" " +
                        "       style=\"display: inline-block; padding: 10px 20px; font-size: 16px; color: white; background-color: #E65787; text-decoration: none; border-radius: 5px; font-weight: bold;\">" +
                        "      설문 참여하기" +
                        "    </a>" +
                        "  </p>" +
                        "  <p style=\"margin-top: 30px;\">감사합니다.<br/><b>Team HaRu 드림</b></p>" +
                        "</body>" +
                        "</html>",
                description, link
        );
    }
}
