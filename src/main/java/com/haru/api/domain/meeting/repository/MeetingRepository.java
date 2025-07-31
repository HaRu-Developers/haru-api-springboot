package com.haru.api.domain.meeting.repository;

import com.haru.api.domain.meeting.entity.Meeting;
import com.haru.api.domain.workspace.dto.WorkspaceResponseDTO;
import com.haru.api.domain.workspace.entity.Workspace;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    List<Meeting> findByWorkspaceOrderByUpdatedAtDesc(Workspace workspace);

    @Query("SELECT new com.haru.api.domain.workspace.dto.WorkspaceResponseDTO$Document(" +
            "udlo.id.documentId, " +
            "mt.title, " +
            "udlo.id.documentType, " +
            "udlo.lastOpened) " +
            "FROM UserDocumentLastOpened udlo " +
            "JOIN Meeting mt ON udlo.id.documentId = mt.id " +
            "WHERE udlo.id.documentType = 'AI_MEETING_MANAGER' AND udlo.user.id = :userId " +
            "AND (:title IS NULL OR :title = '' OR mt.title LIKE %:title%)")
    List<WorkspaceResponseDTO.Document> findRecentDocumentsByTitle(Long userId, String title);


    @Query("SELECT m.workspace FROM Meeting m WHERE m.id = :meetingId")
    Optional<Workspace> findWorkspaceByMeetingId(@Param("meetingId") Long meetingId);


    List<Meeting> findAllByWorkspaceId(Long workspaceId);

}
