package com.haru.api.domain.moodTracker.repository;

import com.haru.api.domain.moodTracker.entity.SubjectiveAnswer;
import com.haru.api.domain.moodTracker.entity.SurveyQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectiveAnswerRepository extends JpaRepository<SubjectiveAnswer, Long> {
    List<SubjectiveAnswer> findAllBySurveyQuestionIn(List<SurveyQuestion> questions);
}
