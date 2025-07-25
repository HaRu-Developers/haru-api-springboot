package com.haru.api.domain.moodTracker.service;

import com.haru.api.domain.moodTracker.converter.MoodTrackerConverter;
import com.haru.api.domain.moodTracker.dto.MoodTrackerResponseDTO;
import com.haru.api.domain.moodTracker.entity.MoodTracker;
import com.haru.api.domain.moodTracker.repository.MoodTrackerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MoodTrackerQueryServiceImpl implements MoodTrackerQueryService {
    private final MoodTrackerRepository moodTrackerRepository;

    @Override
    public MoodTrackerResponseDTO.PreviewList getMoodTrackerPreviewList(Long workspaceId) {
        List<MoodTracker> foundMoodTrackers = moodTrackerRepository.findAllByWorkspaceId(workspaceId);
        MoodTrackerResponseDTO.PreviewList previewList = MoodTrackerConverter.toPreviewListDTO(foundMoodTrackers);
        return previewList;
    }
}
