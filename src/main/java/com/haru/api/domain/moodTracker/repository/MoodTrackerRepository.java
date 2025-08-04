package com.haru.api.domain.moodTracker.repository;

import com.haru.api.domain.moodTracker.entity.MoodTracker;
import com.haru.api.domain.workspace.dto.WorkspaceResponseDTO;
import com.haru.api.domain.workspace.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MoodTrackerRepository extends JpaRepository<MoodTracker, Long> {
    List<MoodTracker> findAllByWorkspaceId(Long workspaceId);

    @Query("SELECT new com.haru.api.domain.workspace.dto.WorkspaceResponseDTO$DocumentCalendar(" +
            "mt.id, " +
            "mt.title, " +
            "com.haru.api.domain.lastOpened.entity.enums.DocumentType.TEAM_MOOD_TRACKER, " +
            "mt.createdAt) " +
            "FROM MoodTracker mt " +
            "WHERE mt.workspace.id = :workspaceId " +
            "AND mt.createdAt BETWEEN :startDate AND :endDate")
    List<WorkspaceResponseDTO.DocumentCalendar> findAllDocumentForCalendars(Long workspaceId, LocalDateTime startDate, LocalDateTime endDate);

    @Modifying
    @Transactional
    @Query("UPDATE MoodTracker m SET m.respondentsNum = m.respondentsNum + 1 WHERE m.id = :moodTrackerId")
    void addRespondentsNum(Long moodTrackerId);

}
