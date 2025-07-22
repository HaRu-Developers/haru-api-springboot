package com.haru.api.domain.moodTracker.repository;

import com.haru.api.domain.moodTracker.entity.CheckboxChoiceAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CheckboxChoiceAnswerRepository extends JpaRepository<CheckboxChoiceAnswer, Long> {
}
