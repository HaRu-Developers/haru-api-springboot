package com.haru.api.domain.term.repository;

import com.haru.api.domain.term.entity.Term;
import com.haru.api.domain.term.entity.enums.TermType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TermRepository extends JpaRepository<Term, Long> {
    Optional<Term> findByType(TermType type);
}
