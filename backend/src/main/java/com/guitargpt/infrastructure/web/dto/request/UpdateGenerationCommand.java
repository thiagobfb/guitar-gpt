package com.guitargpt.infrastructure.web.dto.request;

import com.guitargpt.domain.model.GenerationRequestStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateGenerationCommand(
        @NotNull(message = "Status is required")
        GenerationRequestStatus status,

        @Size(max = 50000, message = "Result text must be at most 50000 characters")
        String resultText,

        @Size(max = 2000, message = "Error message must be at most 2000 characters")
        String errorMessage
) {
}
