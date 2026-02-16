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
                TrackType.valueOf(entity.getType()),
                entity.getDescription(),
                entity.getCreatedAt()
        );
    }

    public TrackJpaEntity toEntity(Track domain) {
        TrackJpaEntity entity = new TrackJpaEntity();
        entity.setId(domain.getId());
        entity.setProjectId(domain.getProjectId());
        entity.setName(domain.getName());
        entity.setType(domain.getType().name());
        entity.setDescription(domain.getDescription());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }
}
