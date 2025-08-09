package com.haru.api.infra.api.repository;

import com.haru.api.infra.api.entity.AIQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AIQuestionRepository extends JpaRepository<AIQuestion, Integer> {
}
