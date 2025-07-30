package com.haru.api.domain.workspaceInvitation.repository;

import com.haru.api.domain.workspaceInvitation.entity.WorkspaceInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkspaceInvitationRepository extends JpaRepository<WorkspaceInvitation, Long> {
    Optional<WorkspaceInvitation> findByToken(String token);
}
