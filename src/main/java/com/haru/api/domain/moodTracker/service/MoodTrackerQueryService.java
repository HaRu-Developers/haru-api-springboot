package com.haru.api.domain.moodTracker.service;

import com.haru.api.domain.moodTracker.dto.MoodTrackerResponseDTO;

import java.util.List;

public interface MoodTrackerQueryService {
    List<MoodTrackerResponseDTO.Preview> getMoodTrackerPreviewList(Long workspaceId);
}
