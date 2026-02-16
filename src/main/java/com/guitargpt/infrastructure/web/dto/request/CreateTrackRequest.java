package com.guitargpt.infrastructure.web.dto.request;

import com.guitargpt.domain.model.TrackType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTrackRequest(
        @NotBlank(message = "Name is required")
        String name,

        @NotNull(message = "Type is required")
        TrackType type,

        String description
) {
}
