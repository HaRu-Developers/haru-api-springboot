package com.haru.api.domain.meeting.repository;

import com.haru.api.domain.meeting.entity.Meetings;
import com.haru.api.domain.workspace.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MeetingRepository extends JpaRepository<Meetings, Long> {
    List<Meeting> findByWorkspaceOrderByUpdatedAtDesc(Workspace workspace);

    @Query("SELECT new com.haru.api.domain.workspace.dto.WorkspaceResponseDTO$Document(" +
            "mt.id, " +
            "mt.title, " +
            "'AI_MEETING_MANAGER', " +
            "null) " +
            "FROM Meetings mt " +
            "WHERE mt.title LIKE %:title% " +
            "AND mt.workspace.id = :workspaceId")
    List<WorkspaceResponseDTO.Document> findDocumentsByTitleLike(String title, Long workspaceId);
}
