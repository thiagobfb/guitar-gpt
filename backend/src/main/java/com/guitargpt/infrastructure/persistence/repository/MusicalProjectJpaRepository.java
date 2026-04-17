package com.guitargpt.infrastructure.persistence.repository;

import com.guitargpt.infrastructure.persistence.entity.MusicalProjectJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MusicalProjectJpaRepository extends JpaRepository<MusicalProjectJpaEntity, UUID> {

    List<MusicalProjectJpaEntity> findByUserId(UUID userId);
}
