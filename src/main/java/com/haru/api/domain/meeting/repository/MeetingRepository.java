package com.haru.api.domain.meeting.repository;

import com.haru.api.domain.meeting.entity.Meetings;
import com.haru.api.domain.workspace.dto.WorkspaceResponseDTO;
import com.haru.api.domain.workspace.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MeetingRepository extends JpaRepository<Meetings, Long> {
    List<Meetings> findByWorkspacesOrderByUpdatedAtDesc(Workspace workspace);

    @Query("SELECT new com.haru.api.domain.workspace.dto.WorkspaceResponseDTO$Document(" +
            "mt.id, " +
            "mt.title, " +
            "'AI_MEETING_MANAGER', " +
            "null) " +
            "FROM Meetings mt " +
            "WHERE mt.title LIKE %:title%")
    List<WorkspaceResponseDTO.Document> findDocumentsByTitleLike(String title);
}
