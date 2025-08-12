package com.haru.api.domain.moodTracker.repository;

import com.haru.api.domain.moodTracker.entity.MoodTracker;
import com.haru.api.domain.workspace.entity.Workspace;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MoodTrackerRepository extends JpaRepository<MoodTracker, Long> {
    List<MoodTracker> findAllByWorkspaceId(Long workspaceId);
  
    @Modifying
    @Transactional
    @Query("UPDATE MoodTracker m SET m.respondentsNum = m.respondentsNum + 1 WHERE m.id = :moodTrackerId")
    void addRespondentsNum(Long moodTrackerId);


    @Query("SELECT mt FROM MoodTracker mt " +
            "WHERE mt.workspace = :workspace " +
            "AND mt.createdAt BETWEEN :startDate AND :endDate")
    List<MoodTracker> findAllDocumentForCalendars(Workspace workspace, LocalDateTime startDate, LocalDateTime endDate);
}
