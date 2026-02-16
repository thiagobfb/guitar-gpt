package com.guitargpt.infrastructure.persistence.mapper;

import com.guitargpt.domain.model.GenerationRequest;
import com.guitargpt.domain.model.GenerationRequestStatus;
import com.guitargpt.infrastructure.persistence.entity.GenerationRequestJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class GenerationRequestMapper {

    public GenerationRequest toDomain(GenerationRequestJpaEntity entity) {
        return new GenerationRequest(
                entity.getId(),
                entity.getProjectId(),
                entity.getPromptTemplateId(),
                entity.getUserPrompt(),
                GenerationRequestStatus.valueOf(entity.getStatus()),
                entity.getResultText(),
                entity.getErrorMessage(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public GenerationRequestJpaEntity toEntity(GenerationRequest domain) {
        GenerationRequestJpaEntity entity = new GenerationRequestJpaEntity();
        entity.setId(domain.getId());
        entity.setProjectId(domain.getProjectId());
        entity.setPromptTemplateId(domain.getPromptTemplateId());
        entity.setUserPrompt(domain.getUserPrompt());
        entity.setStatus(domain.getStatus().name());
        entity.setResultText(domain.getResultText());
        entity.setErrorMessage(domain.getErrorMessage());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
