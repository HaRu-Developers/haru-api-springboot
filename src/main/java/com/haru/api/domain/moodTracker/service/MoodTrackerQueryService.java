package com.haru.api.domain.moodTracker.service;

import com.haru.api.domain.moodTracker.dto.MoodTrackerResponseDTO;

public interface MoodTrackerQueryService {
    MoodTrackerResponseDTO.PreviewList getMoodTrackerPreviewList(Long userId, Long workspaceId);

    MoodTrackerResponseDTO.QuestionResult getQuestionResult(Long userId, Long moodTrackerId);

    MoodTrackerResponseDTO.ReportResult getReportResult(Long userId, Long moodTrackerId);

    MoodTrackerResponseDTO.ResponseResult getResponseResult(Long userId, Long moodTrackerId);
}
