package com.haru.api.domain.moodTracker.service;

import com.haru.api.domain.moodTracker.dto.MoodTrackerRequestDTO;
import com.haru.api.domain.moodTracker.dto.MoodTrackerResponseDTO;
import com.haru.api.domain.moodTracker.entity.MoodTracker;
import com.haru.api.domain.snsEvent.entity.enums.Format;
import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.workspace.entity.Workspace;

public interface MoodTrackerCommandService {
    MoodTrackerResponseDTO.CreateResult create(
            User user,
            Workspace workspace,
            MoodTrackerRequestDTO.CreateRequest request
    );
    void updateTitle(
            User user,
            MoodTracker moodTracker,
            MoodTrackerRequestDTO.UpdateTitleRequest request
    );
    void delete(
            User user,
            MoodTracker moodTracker
    );
    void sendSurveyLink(
            MoodTracker moodTracker
    );
    void submitSurveyAnswers(
            MoodTracker moodTracker,
            MoodTrackerRequestDTO.SurveyAnswerList request
    );
    MoodTrackerResponseDTO.ReportDownLoadLinkResponse getDownloadLink(
            User user,
            MoodTracker moodTracker,
            Format format
    );
    void generateReportTest(
            MoodTracker moodTracker
    );
    void generateReportFileAndThumbnailTest(
            MoodTracker moodTracker
    );
}
