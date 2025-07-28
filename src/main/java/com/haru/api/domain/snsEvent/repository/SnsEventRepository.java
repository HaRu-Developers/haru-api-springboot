package com.haru.api.domain.snsEvent.repository;

import com.haru.api.domain.snsEvent.entity.SnsEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SnsEventRepository extends JpaRepository<SnsEvent, Long> {
}
