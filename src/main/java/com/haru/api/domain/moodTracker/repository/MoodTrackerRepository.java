package com.haru.api.domain.moodTracker.repository;

import com.haru.api.domain.moodTracker.entity.MoodTracker;
import com.haru.api.domain.workspace.dto.WorkspaceResponseDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MoodTrackerRepository extends JpaRepository<MoodTracker, Long> {
    List<MoodTracker> findAllByWorkspaceId(Long workspaceId);

    @Query("SELECT new com.haru.api.domain.workspace.dto.WorkspaceResponseDTO$Document(" +
            "mt.id, " +
            "mt.title, " +
            "'TEAM_MOOD_TRACKER', " +
            "null) " +
            "FROM MoodTracker mt " +
            "WHERE mt.title LIKE %:title%")
    List<WorkspaceResponseDTO.Document> findDocumentsByTitleLike(String title);
}
