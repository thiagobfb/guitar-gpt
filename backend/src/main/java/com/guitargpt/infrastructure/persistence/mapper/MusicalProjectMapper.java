package com.guitargpt.infrastructure.persistence.mapper;

import com.guitargpt.domain.model.MusicalProject;
import com.guitargpt.infrastructure.persistence.entity.MusicalProjectJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class MusicalProjectMapper {

    public MusicalProject toDomain(MusicalProjectJpaEntity entity) {
        return new MusicalProject(
                entity.getId(),
                entity.getUserId(),
                entity.getName(),
                entity.getDescription(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public MusicalProjectJpaEntity toEntity(MusicalProject domain) {
        MusicalProjectJpaEntity entity = new MusicalProjectJpaEntity();
        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        entity.setName(domain.getName());
        entity.setDescription(domain.getDescription());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
