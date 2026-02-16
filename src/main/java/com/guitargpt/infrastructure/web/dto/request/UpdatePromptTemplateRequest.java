package com.guitargpt.infrastructure.web.dto.request;

import com.guitargpt.domain.model.PromptTemplateCategory;
import jakarta.validation.constraints.NotBlank;

public record UpdatePromptTemplateRequest(
        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Template text is required")
        String templateText,

        String description,

        PromptTemplateCategory category
) {
}
