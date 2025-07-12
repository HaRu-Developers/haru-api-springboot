package com.haru.api.domain.userWorkspace.repository;

import com.haru.api.domain.userWorkspace.entity.UserWorkspace;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserWorkspaceRepository extends JpaRepository<UserWorkspace, Long> {
}
