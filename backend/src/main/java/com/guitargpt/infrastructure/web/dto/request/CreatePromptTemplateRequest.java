package com.guitargpt.infrastructure.web.dto.request;

import com.guitargpt.domain.model.PromptTemplateCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePromptTemplateRequest(
        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Template text is required")
        String templateText,

        String description,

        @NotNull(message = "Category is required")
        PromptTemplateCategory category
) {
}
