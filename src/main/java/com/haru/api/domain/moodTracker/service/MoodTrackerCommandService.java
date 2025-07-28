package com.haru.api.domain.moodTracker.service;

import com.haru.api.domain.moodTracker.dto.MoodTrackerRequestDTO;
import com.haru.api.domain.moodTracker.dto.MoodTrackerResponseDTO;

public interface MoodTrackerCommandService {
    MoodTrackerResponseDTO.CreateResult create(
            Long userId,
            Long workspaceId,
            MoodTrackerRequestDTO.CreateRequest request
    );
    void updateTitle(
            Long userId,
            Long moodTrackerId,
            MoodTrackerRequestDTO.UpdateTitleRequest request
    );
    void delete(
            Long userId,
            Long moodTrackerId
    );
    void sendSurveyLink(
            Long moodTrackerId
    );
    void submitSurveyAnswers(
            Long moodTrackerId,
            MoodTrackerRequestDTO.SurveyAnswerList request
    );
}
