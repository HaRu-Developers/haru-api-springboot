package com.haru.api.domain.workspace.repository;

import com.haru.api.domain.workspace.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
    @Query("SELECT m.workspace FROM MoodTracker m WHERE m.id = :moodTrackerId")
    Optional<Workspace> findByMoodTrackerId(@Param("moodTrackerId") Long moodTrackerId);
}
