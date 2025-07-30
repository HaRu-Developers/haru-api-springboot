package com.haru.api.domain.meeting.repository;

import com.haru.api.domain.meeting.entity.Meeting;
import com.haru.api.domain.workspace.dto.WorkspaceResponseDTO;
import com.haru.api.domain.workspace.entity.Workspace;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    List<Meeting> findByWorkspaceOrderByUpdatedAtDesc(Workspace workspace);

    @Query("SELECT new com.haru.api.domain.workspace.dto.WorkspaceResponseDTO$Document(" +
            "udlo.id.documentId, " +
            "mt.title, " +
            "udlo.id.documentType, " +
            "udlo.lastOpened) " +
            "FROM UserDocumentLastOpened  udlo " +
            "JOIN Meeting mt ON udlo.documentId = mt.id " +
            "WHERE udlo.id.documentType = 'AI_MEETING_MANAGER' AND udlo.user.id = :userId " +
            "AND mt.title LIKE %:title% " +
            "ORDER BY udlo.lastOpened DESC")
    List<WorkspaceResponseDTO.Document> findRecentDocumentsByTitle(Long userId, String title, Pageable pageable);

    List<Meeting> findAllByWorkspaceId(Long workspaceId);
}
