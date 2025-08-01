package com.haru.api.domain.snsEvent.repository;

import com.haru.api.domain.snsEvent.entity.SnsEvent;
import com.haru.api.domain.workspace.dto.WorkspaceResponseDTO;
import com.haru.api.domain.workspace.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SnsEventRepository extends JpaRepository<SnsEvent, Long> {

    @Query("SELECT new com.haru.api.domain.workspace.dto.WorkspaceResponseDTO$Document(" +
            "udlo.id.documentId, " +
            "se.title, " +
            "udlo.id.documentType, " +
            "udlo.lastOpened) " +
            "FROM UserDocumentLastOpened  udlo " +
            "JOIN SnsEvent se ON udlo.id.documentId = se.id " +
            "WHERE se.workspace.id = :workspaceId AND udlo.id.documentType = 'SNS_EVENT_ASSISTANT' AND udlo.user.id = :userId " +
            "AND (:title IS NULL OR :title = '' OR se.title LIKE %:title%)")
    List<WorkspaceResponseDTO.Document> findRecentDocumentsByTitle(Long workspaceId, Long userId, String title);

    List<SnsEvent> findAllByWorkspaceId(Long workspaceId);

    List<SnsEvent> findAllByWorkspace(Workspace foundWorkspace);

    @Query("SELECT new com.haru.api.domain.workspace.dto.WorkspaceResponseDTO$DocumentCalendar(" +
            "se.id, " +
            "se.title, " +
            "com.haru.api.domain.lastOpened.entity.enums.DocumentType.SNS_EVENT_ASSISTANT, " +
            "se.createdAt) " +
            "FROM SnsEvent se " +
            "WHERE se.workspace.id = :workspaceId " +
            "AND se.createdAt BETWEEN :startDate AND :endDate")
    List<WorkspaceResponseDTO.DocumentCalendar> findAllDocumentForCalendars(Long workspaceId, LocalDateTime startDate, LocalDateTime endDate);

}
