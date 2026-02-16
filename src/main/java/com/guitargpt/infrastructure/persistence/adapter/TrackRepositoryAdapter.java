package com.guitargpt.infrastructure.persistence.adapter;

import com.guitargpt.domain.model.Track;
import com.guitargpt.domain.port.out.TrackRepository;
import com.guitargpt.infrastructure.persistence.mapper.TrackMapper;
import com.guitargpt.infrastructure.persistence.repository.TrackJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TrackRepositoryAdapter implements TrackRepository {

    private final TrackJpaRepository jpaRepository;
    private final TrackMapper mapper;

    public TrackRepositoryAdapter(TrackJpaRepository jpaRepository, TrackMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Track save(Track track) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(track)));
    }

    @Override
    public Optional<Track> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Track> findByProjectId(UUID projectId) {
        return jpaRepository.findByProjectId(projectId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public long countByProjectId(UUID projectId) {
        return jpaRepository.countByProjectId(projectId);
    }
}
