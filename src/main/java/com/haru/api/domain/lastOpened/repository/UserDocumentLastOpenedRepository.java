package com.haru.api.domain.lastOpened.repository;

import com.haru.api.domain.lastOpened.entity.UserDocumentLastOpened;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserDocumentLastOpenedRepository extends JpaRepository<UserDocumentLastOpened, Long> {

    List<UserDocumentLastOpened> findTop9ByUserIdOrderByLastOpenedDateDesc(Long userId);
}
