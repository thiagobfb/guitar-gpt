package com.guitargpt.infrastructure.web.dto.request;

import com.guitargpt.domain.model.PromptTemplateCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePromptTemplateRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must be at most 255 characters")
        String name,

        @NotBlank(message = "Template text is required")
        @Size(max = 10000, message = "Template text must be at most 10000 characters")
        String templateText,

        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        @NotNull(message = "Category is required")
        PromptTemplateCategory category
) {
}
