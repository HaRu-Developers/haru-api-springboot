package com.haru.api.domain.moodTracker.service;

import com.haru.api.domain.moodTracker.dto.MoodTrackerResponseDTO;
import com.haru.api.domain.moodTracker.entity.MoodTracker;
import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.workspace.entity.Workspace;

public interface MoodTrackerQueryService {
    MoodTrackerResponseDTO.PreviewList getPreviewList(User user, Workspace workspace);

    MoodTrackerResponseDTO.BaseResult getBaseResult(User user, MoodTracker moodTracker);

    MoodTrackerResponseDTO.QuestionResult getQuestionResult(User user, MoodTracker moodTracker);

    MoodTrackerResponseDTO.ReportResult getReportResult(User user, MoodTracker moodTracker);

    MoodTrackerResponseDTO.ResponseResult getResponseResult(User user, MoodTracker moodTracker);
}
