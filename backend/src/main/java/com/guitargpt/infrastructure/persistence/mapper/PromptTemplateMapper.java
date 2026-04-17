package com.guitargpt.infrastructure.persistence.mapper;

import com.guitargpt.domain.model.PromptTemplate;
import com.guitargpt.domain.model.PromptTemplateCategory;
import com.guitargpt.infrastructure.persistence.entity.PromptTemplateJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class PromptTemplateMapper {

    public PromptTemplate toDomain(PromptTemplateJpaEntity entity) {
        return new PromptTemplate(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getTemplateText(),
                entity.getCategory() != null ? PromptTemplateCategory.valueOf(entity.getCategory()) : null,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public PromptTemplateJpaEntity toEntity(PromptTemplate domain) {
        PromptTemplateJpaEntity entity = new PromptTemplateJpaEntity();
        entity.setId(domain.getId());
        entity.setName(domain.getName());
        entity.setDescription(domain.getDescription());
        entity.setTemplateText(domain.getTemplateText());
        entity.setCategory(domain.getCategory() != null ? domain.getCategory().name() : null);
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
