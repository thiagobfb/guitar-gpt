package com.guitargpt.infrastructure.persistence.adapter;

import com.guitargpt.domain.model.MusicalProject;
import com.guitargpt.domain.port.out.MusicalProjectRepository;
import com.guitargpt.infrastructure.persistence.mapper.MusicalProjectMapper;
import com.guitargpt.infrastructure.persistence.repository.MusicalProjectJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MusicalProjectRepositoryAdapter implements MusicalProjectRepository {

    private final MusicalProjectJpaRepository jpaRepository;
    private final MusicalProjectMapper mapper;

    public MusicalProjectRepositoryAdapter(MusicalProjectJpaRepository jpaRepository, MusicalProjectMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public MusicalProject save(MusicalProject project) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(project)));
    }

    @Override
    public Optional<MusicalProject> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<MusicalProject> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
