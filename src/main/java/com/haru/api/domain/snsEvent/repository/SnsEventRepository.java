package com.haru.api.domain.snsEvent.repository;

import com.haru.api.domain.snsEvent.entity.SnsEvent;
import com.haru.api.domain.workspace.dto.WorkspaceResponseDTO;
import com.haru.api.domain.workspace.entity.Workspace;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

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
            "WHERE udlo.id.documentType = 'SNS_EVENT_ASSISTANT' AND udlo.user.id = :userId " +
            "AND (:title IS NULL OR :title = '' OR se.title LIKE %:title%)")
    List<WorkspaceResponseDTO.Document> findRecentDocumentsByTitle(Long userId, String title);

    List<SnsEvent> findAllByWorkspaceId(Long workspaceId);

    List<SnsEvent> findAllByWorkspace(Workspace foundWorkspace);
}
