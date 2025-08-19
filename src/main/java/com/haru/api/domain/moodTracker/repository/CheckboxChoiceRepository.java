package com.haru.api.domain.moodTracker.repository;

import com.haru.api.domain.moodTracker.entity.CheckboxChoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CheckboxChoiceRepository extends JpaRepository<CheckboxChoice, Long> {
    // 특정 질문(questionId)에 속한 여러 선택지(ids) 조회
    List<CheckboxChoice> findAllByIdInAndSurveyQuestionId(List<Long> ids, Long questionId);
}
