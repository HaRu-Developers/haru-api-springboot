package com.haru.api.domain.snsEvent.repository;

import com.haru.api.domain.snsEvent.entity.SnsEvent;
import com.haru.api.domain.workspace.dto.WorkspaceResponseDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SnsEventRepository extends JpaRepository<SnsEvent, Long> {

    @Query("SELECT new com.haru.api.domain.workspace.dto.WorkspaceResponseDTO$Document(" +
            "udlo.documentId, " +
            "se.title, " +
            "udlo.documentType, " +
            "udlo.lastOpened) " +
            "FROM UserDocumentLastOpened  udlo " +
            "JOIN SnsEvent se ON udlo.documentId = se.id " +
            "WHERE udlo.documentType = 'SNS_EVENT_ASSISTANT' AND udlo.user.id = :userId " +
            "AND se.title LIKE %:title% " +
            "ORDER BY udlo.lastOpened DESC")
    List<WorkspaceResponseDTO.Document> findRecentDocumentsByTitle(Long userId, String title, Pageable pageable);
}
