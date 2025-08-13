package com.haru.api.infra.api.repository;

import com.haru.api.domain.meeting.entity.Meeting;
import com.haru.api.infra.api.entity.SpeechSegment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpeechSegmentRepository extends JpaRepository<SpeechSegment, Long> {
    // 특정 Meeting에 속한 모든 SpeechSegment를 조회하는 메서드
    List<SpeechSegment> findByMeeting(Meeting meeting);
}
