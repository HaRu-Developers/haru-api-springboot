package com.haru.api.domain.moodTracker.service;

import com.haru.api.domain.moodTracker.dto.MoodTrackerResponseDTO;
import com.haru.api.domain.moodTracker.entity.MoodTracker;
import com.haru.api.domain.moodTracker.repository.MoodTrackerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MoodTrackerQueryServiceImpl implements MoodTrackerQueryService {
    private final MoodTrackerRepository moodTrackerRepository;

    @Override
    public List<MoodTrackerResponseDTO.Preview> getMoodTrackerPreviewList(Long workspaceId){

        List<MoodTracker> moodTrackers = moodTrackerRepository.findAllByWorkspaceId(workspaceId);

        return moodTrackers.stream()
                .map(moodTracker -> MoodTrackerResponseDTO.Preview.builder()
                        .moodTrackerId(moodTracker.getId())
                        .title(moodTracker.getTitle())
                        .createdAt(moodTracker.getCreatedAt())
                        .dueDate(moodTracker.getDueDate())
                        .respondentsNum(moodTracker.getRespondentsNum())
                        .build()
                )
                .collect(Collectors.toList());
    }
}
