package com.haru.api.domain.moodTracker.repository;

import com.haru.api.domain.moodTracker.entity.CheckboxChoiceAnswer;
import com.haru.api.domain.moodTracker.entity.SurveyQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CheckboxChoiceAnswerRepository extends JpaRepository<CheckboxChoiceAnswer, Long> {
    List<CheckboxChoiceAnswer> findAllByCheckboxChoice_SurveyQuestionIn(List<SurveyQuestion> questions);
}
