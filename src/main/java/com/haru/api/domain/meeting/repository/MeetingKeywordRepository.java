package com.haru.api.domain.meeting.repository;

import com.haru.api.domain.meeting.entity.MeetingKeyword;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingKeywordRepository extends JpaRepository<MeetingKeyword, Long> {
}
