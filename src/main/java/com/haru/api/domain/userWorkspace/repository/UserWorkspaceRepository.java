package com.haru.api.domain.userWorkspace.repository;

import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.userWorkspace.dto.UserWorkspaceResponseDTO;
import com.haru.api.domain.userWorkspace.entity.UserWorkspace;
import com.haru.api.domain.userWorkspace.entity.enums.Auth;
import com.haru.api.domain.workspace.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserWorkspaceRepository extends JpaRepository<UserWorkspace, Long> {

    @Query("SELECT new com.haru.api.domain.userWorkspace.dto.UserWorkspaceResponseDTO$UserWorkspaceWithTitle(" +
            "uw.workspace.id, " +
            "uw.workspace.title, " +
            "uw.workspace.imageUrl, " +
            "CASE WHEN uw.auth = 'ADMIN' THEN true ELSE false END) " +
            "FROM UserWorkspace uw " +
            "WHERE uw.user.id = :userId")
    List<UserWorkspaceResponseDTO.UserWorkspaceWithTitle> getUserWorkspacesWithTitle(@Param("userId") Long userId);

    @Query("SELECT uw.user.email FROM UserWorkspace uw WHERE uw.workspace.id = :workspaceId")
    List<String> findEmailsByWorkspaceId(@Param("workspaceId") Long workspaceId);

    Boolean existsByUserIdAndWorkspaceId(Long userId, Long workspaceId);

    Optional<UserWorkspace> findByWorkspaceIdAndUserId(Long workspaceId, Long userId);

    Optional<UserWorkspace> findByUserAndWorkspace(User user, Workspace workspace);

    Optional<UserWorkspace> findByUserIdAndWorkspaceId(Long userId, Long workspaceId);

    Optional<UserWorkspace> findByWorkspaceAndAuth(Workspace workspace, Auth auth);

    @Query("SELECT uw.user FROM UserWorkspace uw WHERE uw.workspace.id = :workspaceId")
    List<User> findUsersByWorkspaceId(Long workspaceId);
}
