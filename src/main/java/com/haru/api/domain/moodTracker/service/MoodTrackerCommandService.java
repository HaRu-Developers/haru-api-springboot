package com.haru.api.domain.moodTracker.service;

import com.haru.api.domain.moodTracker.dto.MoodTrackerRequestDTO;
import com.haru.api.domain.moodTracker.dto.MoodTrackerResponseDTO;

import java.util.List;

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
}
