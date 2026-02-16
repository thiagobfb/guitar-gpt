package com.guitargpt.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateGenerationRequestRequest(
        @NotNull(message = "Prompt template ID is required")
        UUID promptTemplateId,

        @NotBlank(message = "User prompt is required")
        String userPrompt
) {
}
