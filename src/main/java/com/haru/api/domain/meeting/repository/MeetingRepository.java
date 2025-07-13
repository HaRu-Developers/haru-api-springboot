package com.haru.api.domain.meeting.repository;

import com.haru.api.domain.meeting.entity.Meetings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MeetingRepository extends JpaRepository<Meetings, Long> {
}
