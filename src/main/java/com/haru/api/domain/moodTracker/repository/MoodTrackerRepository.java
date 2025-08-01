package com.haru.api.domain.moodTracker.repository;

import com.haru.api.domain.moodTracker.entity.MoodTracker;
import com.haru.api.domain.workspace.dto.WorkspaceResponseDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

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

    @Query("SELECT new com.haru.api.domain.workspace.dto.WorkspaceResponseDTO$DocumentCalendar(" +
            "mt.id, " +
            "mt.title, " +
            "com.haru.api.domain.lastOpened.entity.enums.DocumentType.TEAM_MOOD_TRACKER, " +
            "mt.createdAt) " +
            "FROM MoodTracker mt " +
            "WHERE mt.workspace.id = :workspaceId " +
            "AND mt.createdAt BETWEEN :startDate AND :endDate")
    List<WorkspaceResponseDTO.DocumentCalendar> findAllDocumentForCalendars(Long workspaceId, LocalDateTime startDate, LocalDateTime endDate);
}
