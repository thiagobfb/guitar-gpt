package com.guitargpt.infrastructure.persistence.repository;

import com.guitargpt.infrastructure.persistence.entity.TrackJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TrackJpaRepository extends JpaRepository<TrackJpaEntity, UUID> {

    List<TrackJpaEntity> findByProjectId(UUID projectId);

    long countByProjectId(UUID projectId);
}
