package com.haru.api.domain.meeting.repository;

import com.haru.api.domain.meeting.entity.MeetingTag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingTagRepository extends JpaRepository<MeetingTag, Long> {
}
