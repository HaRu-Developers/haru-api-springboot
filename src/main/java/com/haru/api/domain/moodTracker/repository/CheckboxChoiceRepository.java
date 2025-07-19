package com.haru.api.domain.moodTracker.repository;

import com.haru.api.domain.moodTracker.entity.CheckboxChoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CheckboxChoiceRepository extends JpaRepository<CheckboxChoice, Long> {
}
