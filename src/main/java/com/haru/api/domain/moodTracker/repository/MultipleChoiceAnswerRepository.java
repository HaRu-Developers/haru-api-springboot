package com.haru.api.domain.moodTracker.repository;

import com.haru.api.domain.moodTracker.entity.MultipleChoiceAnswer;
import com.haru.api.domain.moodTracker.entity.SurveyQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MultipleChoiceAnswerRepository extends JpaRepository<MultipleChoiceAnswer, Long> {
    List<MultipleChoiceAnswer> findAllByMultipleChoice_SurveyQuestionIn(List<SurveyQuestion> questions);
}
