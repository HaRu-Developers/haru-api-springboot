package com.haru.api.domain.user.repository;

import com.haru.api.domain.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<Users, Long> {
    List<Users> findTop4UsersByEmailContainingIgnoreCase(String email); // <<< 이 부분은 내 브랜치에서 추가된 메서드
    Optional<Users> findByEmail(String email);
}
