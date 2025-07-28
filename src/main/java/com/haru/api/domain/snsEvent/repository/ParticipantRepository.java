package com.haru.api.domain.snsEvent.repository;

import com.haru.api.domain.snsEvent.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {
}
