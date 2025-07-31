package com.haru.api.domain.snsEvent.repository;

import com.haru.api.domain.snsEvent.entity.SnsEvent;
import com.haru.api.domain.snsEvent.entity.Winner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WinnerRepository extends JpaRepository<Winner, Long> {
    List<Winner> findAllBySnsEvent(SnsEvent foundSnsEvent);
}
