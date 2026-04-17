package com.guitargpt.infrastructure.persistence.repository;

import com.guitargpt.infrastructure.persistence.entity.GenerationRequestJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GenerationRequestJpaRepository extends JpaRepository<GenerationRequestJpaEntity, UUID> {

    List<GenerationRequestJpaEntity> findByProjectId(UUID projectId);

    List<GenerationRequestJpaEntity> findByStatus(String status);
}
