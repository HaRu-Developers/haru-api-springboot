package com.haru.api.domain.meeting.repository;

import com.haru.api.domain.meeting.entity.Meeting;
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
public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    List<Meeting> findByWorkspaceOrderByUpdatedAtDesc(Workspace workspace);

    @Query("SELECT m.workspace FROM Meeting m WHERE m.id = :meetingId")
    Optional<Workspace> findWorkspaceByMeetingId(@Param("meetingId") Long meetingId);


    List<Meeting> findAllByWorkspaceId(Long workspaceId);

    @Query("SELECT new com.haru.api.domain.workspace.dto.WorkspaceResponseDTO$DocumentCalendar(" +
            "mt.id, " +
            "mt.title, " +
            "com.haru.api.domain.lastOpened.entity.enums.DocumentType.AI_MEETING_MANAGER, " +
            "mt.createdAt) " +
            "FROM Meeting mt " +
            "WHERE mt.workspace.id = :workspaceId " +
            "AND mt.createdAt BETWEEN :startDate AND :endDate")
    List<WorkspaceResponseDTO.DocumentCalendar> findAllDocumentForCalendars(Long workspaceId, LocalDateTime startDate, LocalDateTime endDate);

}
