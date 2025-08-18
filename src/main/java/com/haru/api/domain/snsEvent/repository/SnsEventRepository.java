package com.haru.api.domain.snsEvent.repository;

import com.haru.api.domain.snsEvent.entity.SnsEvent;
import com.haru.api.domain.workspace.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SnsEventRepository extends JpaRepository<SnsEvent, Long> {

    @Query("SELECT m FROM SnsEvent m WHERE m.workspace.id = :workspaceId")
    List<SnsEvent> findAllByWorkspaceId(Long workspaceId);

    List<SnsEvent> findAllByWorkspace(Workspace foundWorkspace);


    @Query("SELECT mt FROM SnsEvent mt " +
            "WHERE mt.workspace.id = :workspaceId " +
            "AND mt.createdAt BETWEEN :startDate AND :endDate")
    List<SnsEvent> findAllDocumentForCalendars(Long workspaceId, LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT se FROM SnsEvent se " +
            "WHERE se.id = :snsEventId AND EXISTS (" +
            "  SELECT 1 FROM UserWorkspace uw " +
            "  WHERE uw.user.id = :userId AND uw.workspace.id = se.workspace.id" +
            ")")
    Optional<SnsEvent> findSnsEventByIdIfUserHasAccess(Long userId, Long snsEventId);
}
