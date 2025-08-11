package com.haru.api.domain.snsEvent.repository;

import com.haru.api.domain.snsEvent.entity.SnsEvent;
import com.haru.api.domain.workspace.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SnsEventRepository extends JpaRepository<SnsEvent, Long> {

    List<SnsEvent> findAllByWorkspaceId(Long workspaceId);

    List<SnsEvent> findAllByWorkspace(Workspace foundWorkspace);


    @Query("SELECT mt FROM SnsEvent mt " +
            "WHERE mt.workspace = :workspace " +
            "AND mt.createdAt BETWEEN :startDate AND :endDate")
    List<SnsEvent> findAllDocumentForCalendars(Workspace workspace, LocalDateTime startDate, LocalDateTime endDate);
}
