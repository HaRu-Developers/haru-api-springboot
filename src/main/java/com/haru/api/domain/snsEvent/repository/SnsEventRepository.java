package com.haru.api.domain.snsEvent.repository;

import com.haru.api.domain.snsEvent.entity.SnsEvent;
import com.haru.api.domain.workspace.dto.WorkspaceResponseDTO;
import com.haru.api.domain.workspace.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SnsEventRepository extends JpaRepository<SnsEvent, Long> {

    @Query("SELECT new com.haru.api.domain.workspace.dto.WorkspaceResponseDTO$Document(" +
            "udlo.id.documentId, " +
            "udlo.title, " +
            "udlo.id.documentType, " +
            "udlo.lastOpened) " +
            "FROM UserDocumentLastOpened udlo " +
            "WHERE udlo.workspaceId = :workspaceId AND udlo.id.documentType = 'SNS_EVENT_ASSISTANT' AND udlo.user.id = :userId " +
            "AND (:title IS NULL OR :title = '' OR udlo.title LIKE %:title%)")
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
