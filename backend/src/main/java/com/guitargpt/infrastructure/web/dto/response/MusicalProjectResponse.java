package com.guitargpt.infrastructure.web.dto.response;

import com.guitargpt.domain.model.MusicalProject;

import java.time.LocalDateTime;
import java.util.UUID;

public record MusicalProjectResponse(
        UUID id,
        UUID userId,
        String name,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static MusicalProjectResponse from(MusicalProject project) {
        return new MusicalProjectResponse(
                project.getId(),
                project.getUserId(),
                project.getName(),
                project.getDescription(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
