package com.haru.api.domain.moodTracker.service;

import com.haru.api.domain.moodTracker.dto.MoodTrackerResponseDTO;

public interface MoodTrackerQueryService {
    MoodTrackerResponseDTO.PreviewList getMoodTrackerPreviewList(Long workspaceId);
}
