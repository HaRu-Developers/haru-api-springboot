package com.haru.api.domain.lastOpened.repository;

import com.haru.api.domain.lastOpened.entity.UserDocumentId;
import com.haru.api.domain.lastOpened.entity.UserDocumentLastOpened;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserDocumentLastOpenedRepository extends JpaRepository<UserDocumentLastOpened, Long> {
    Optional<UserDocumentLastOpened> findById(UserDocumentId id);

    List<UserDocumentLastOpened> findTop5ByWorkspaceIdAndUserIdOrderByLastOpenedDesc(Long workspaceId, Long userId);

    @Query("SELECT udlo " +
            "FROM UserDocumentLastOpened udlo " +
            "WHERE udlo.workspaceId = :workspaceId AND udlo.id.userId = :userId " +
            "AND udlo.title LIKE %:title% " +
            "ORDER BY udlo.lastOpened DESC")
    List<UserDocumentLastOpened> findRecentDocumentsByTitle(Long workspaceId, Long userId, String title);
}
