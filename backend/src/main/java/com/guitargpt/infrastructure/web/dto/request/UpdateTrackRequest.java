package com.guitargpt.infrastructure.web.dto.request;

import com.guitargpt.domain.model.TrackType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateTrackRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must be at most 255 characters")
        String name,

        @NotNull(message = "Type is required")
        TrackType type,

        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description
) {
}
