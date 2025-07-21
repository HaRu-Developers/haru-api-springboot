package com.haru.api.domain.snsEvent.repository;

import com.haru.api.domain.snsEvent.entity.SnsEvent;
import com.haru.api.domain.workspace.dto.WorkspaceResponseDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SnsEventRepository extends JpaRepository<SnsEvent, Long> {

    @Query("SELECT new com.haru.api.domain.workspace.dto.WorkspaceResponseDTO$Document(" +
            "se.id, " +
            "se.title, " +
            "'SNS_EVENT_ASSISTANT', " +
            "null) " +
            "FROM SnsEvent se " +
            "WHERE se.title LIKE %:title%")
    List<WorkspaceResponseDTO.Document> findDocumentsByTitleLike(String title);
}
