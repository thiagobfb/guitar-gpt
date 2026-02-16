package com.guitargpt.infrastructure.persistence.repository;

import com.guitargpt.infrastructure.persistence.entity.PromptTemplateJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PromptTemplateJpaRepository extends JpaRepository<PromptTemplateJpaEntity, UUID> {

    List<PromptTemplateJpaEntity> findByCategory(String category);
}
