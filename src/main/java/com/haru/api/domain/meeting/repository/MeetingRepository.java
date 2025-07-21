package com.haru.api.domain.meeting.repository;

import com.haru.api.domain.meeting.entity.Meetings;
import com.haru.api.domain.workspace.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MeetingRepository extends JpaRepository<Meetings, Long> {
    List<Meetings> findByWorkspacesOrderByUpdatedAtDesc(Workspace workspace);
}
