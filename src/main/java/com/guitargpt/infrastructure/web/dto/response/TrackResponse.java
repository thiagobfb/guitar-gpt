package com.guitargpt.infrastructure.web.dto.response;

import com.guitargpt.domain.model.Track;
import com.guitargpt.domain.model.TrackType;

import java.time.LocalDateTime;
import java.util.UUID;

public record TrackResponse(
        UUID id,
        UUID projectId,
        String name,
        TrackType type,
        String description,
        LocalDateTime createdAt
) {
    public static TrackResponse from(Track track) {
        return new TrackResponse(
                track.getId(),
                track.getProjectId(),
                track.getName(),
                track.getType(),
                track.getDescription(),
                track.getCreatedAt()
        );
    }
}
