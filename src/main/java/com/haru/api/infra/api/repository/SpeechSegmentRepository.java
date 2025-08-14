package com.haru.api.infra.api.repository;

import com.haru.api.domain.meeting.entity.Meeting;
import com.haru.api.infra.api.entity.SpeechSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpeechSegmentRepository extends JpaRepository<SpeechSegment, Long> {
    // 특정 Meeting에 속한 모든 SpeechSegment를 조회하는 메서드
    List<SpeechSegment> findByMeeting(Meeting meeting);

    @Query("SELECT DISTINCT s FROM SpeechSegment s LEFT JOIN FETCH s.aiQuestions WHERE s.meeting.id = :meetingId ORDER BY s.startTime ASC")
    List<SpeechSegment> findAllByMeetingIdWithAIQuestions(@Param("meetingId") Long meetingId);
}
