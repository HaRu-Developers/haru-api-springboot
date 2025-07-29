package com.haru.api.infra.s3;

import com.haru.api.global.common.entity.Uuid;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UuidRepository extends JpaRepository<Uuid, String> {
}
