package com.guitargpt.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateMusicalProjectRequest(
        @NotBlank(message = "Name is required")
        String name,

        String description
) {
}
