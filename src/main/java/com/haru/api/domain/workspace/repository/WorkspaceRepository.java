package com.haru.api.domain.workspace.repository;

import com.haru.api.domain.workspace.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
}
