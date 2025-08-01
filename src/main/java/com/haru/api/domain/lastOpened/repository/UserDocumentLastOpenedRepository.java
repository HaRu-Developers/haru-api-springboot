package com.haru.api.domain.lastOpened.repository;

import com.haru.api.domain.lastOpened.entity.UserDocumentId;
import com.haru.api.domain.lastOpened.entity.UserDocumentLastOpened;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserDocumentLastOpenedRepository extends JpaRepository<UserDocumentLastOpened, Long> {
    Optional<UserDocumentLastOpened> findById(UserDocumentId id);
}
