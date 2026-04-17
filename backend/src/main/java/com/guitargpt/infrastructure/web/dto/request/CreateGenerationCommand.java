package com.guitargpt.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateGenerationCommand(
        @NotNull(message = "Prompt template ID is required")
        UUID promptTemplateId,

        @NotBlank(message = "User prompt is required")
        @Size(max = 5000, message = "User prompt must be at most 5000 characters")
        String userPrompt
) {
}
