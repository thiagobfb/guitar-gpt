package com.guitargpt.infrastructure.web.dto.response;

import com.guitargpt.domain.model.PromptTemplate;
import com.guitargpt.domain.model.PromptTemplateCategory;

import java.time.LocalDateTime;
import java.util.UUID;

public record PromptTemplateResponse(
        UUID id,
        String name,
        String description,
        String templateText,
        PromptTemplateCategory category,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PromptTemplateResponse from(PromptTemplate promptTemplate) {
        return new PromptTemplateResponse(
                promptTemplate.getId(),
                promptTemplate.getName(),
                promptTemplate.getDescription(),
                promptTemplate.getTemplateText(),
                promptTemplate.getCategory(),
                promptTemplate.getCreatedAt(),
                promptTemplate.getUpdatedAt()
        );
    }
}
