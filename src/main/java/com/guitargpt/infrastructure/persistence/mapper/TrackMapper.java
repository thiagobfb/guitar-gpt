package com.guitargpt.infrastructure.persistence.mapper;

import com.guitargpt.domain.model.Track;
import com.guitargpt.domain.model.TrackType;
import com.guitargpt.infrastructure.persistence.entity.TrackJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class TrackMapper {

    public Track toDomain(TrackJpaEntity entity) {
        return new Track(
                entity.getId(),
                entity.getProjectId(),
                entity.getName(),
                entity.getType() != null ? TrackType.valueOf(entity.getType()) : null,
                entity.getDescription(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public TrackJpaEntity toEntity(Track domain) {
        TrackJpaEntity entity = new TrackJpaEntity();
        entity.setId(domain.getId());
        entity.setProjectId(domain.getProjectId());
        entity.setName(domain.getName());
        entity.setType(domain.getType() != null ? domain.getType().name() : null);
        entity.setDescription(domain.getDescription());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
