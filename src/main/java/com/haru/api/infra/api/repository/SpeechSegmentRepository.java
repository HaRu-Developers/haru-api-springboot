package com.haru.api.infra.api.repository;

import com.haru.api.infra.api.entity.SpeechSegment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpeechSegmentRepository extends JpaRepository<SpeechSegment, Long> {
}
