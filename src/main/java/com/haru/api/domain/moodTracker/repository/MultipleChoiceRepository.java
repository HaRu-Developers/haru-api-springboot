package com.haru.api.domain.moodTracker.repository;

import com.haru.api.domain.moodTracker.entity.MultipleChoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MultipleChoiceRepository extends JpaRepository<MultipleChoice, Long> {
}
