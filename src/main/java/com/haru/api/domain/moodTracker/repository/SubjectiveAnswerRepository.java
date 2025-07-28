package com.haru.api.domain.moodTracker.repository;

import com.haru.api.domain.moodTracker.entity.SubjectiveAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubjectiveAnswerRepository extends JpaRepository<SubjectiveAnswer, Long> {
}
