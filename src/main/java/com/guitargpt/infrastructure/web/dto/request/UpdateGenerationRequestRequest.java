package com.guitargpt.infrastructure.web.dto.request;

import com.guitargpt.domain.model.GenerationRequestStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateGenerationRequestRequest(
        @NotNull(message = "Status is required")
        GenerationRequestStatus status,

        String resultText,

        String errorMessage
) {
}
