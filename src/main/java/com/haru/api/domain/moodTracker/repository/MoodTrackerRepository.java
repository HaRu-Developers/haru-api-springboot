package com.haru.api.domain.moodTracker.repository;

import com.haru.api.domain.moodTracker.entity.MoodTracker;
import com.haru.api.domain.workspace.dto.WorkspaceResponseDTO;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MoodTrackerRepository extends JpaRepository<MoodTracker, Long> {
    List<MoodTracker> findAllByWorkspaceId(Long workspaceId);

    @Query("SELECT new com.haru.api.domain.workspace.dto.WorkspaceResponseDTO$Document(" +
            "udlo.id.documentId, " +
            "mt.title, " +
            "udlo.id.documentType, " +
            "udlo.lastOpened) " +
            "FROM UserDocumentLastOpened  udlo " +
            "JOIN MoodTracker mt ON udlo.id.documentId = mt.id " +
            "WHERE mt.workspace.id = :workspaceId AND udlo.id.documentType = 'TEAM_MOOD_TRACKER' AND udlo.user.id = :userId " +
            "AND (:title IS NULL OR :title = '' OR mt.title LIKE %:title%)")
    List<WorkspaceResponseDTO.Document> findRecentDocumentsByTitle(Long workspaceId, Long userId, String title);
  
    @Modifying
    @Transactional
    @Query("UPDATE MoodTracker m SET m.respondentsNum = m.respondentsNum + 1 WHERE m.id = :moodTrackerId")
    void addRespondentsNum(Long moodTrackerId);

}
