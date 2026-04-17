package com.guitargpt.infrastructure.persistence.adapter;

import com.guitargpt.domain.model.GenerationRequest;
import com.guitargpt.domain.model.GenerationRequestStatus;
import com.guitargpt.domain.port.out.GenerationRequestRepository;
import com.guitargpt.infrastructure.persistence.mapper.GenerationRequestMapper;
import com.guitargpt.infrastructure.persistence.repository.GenerationRequestJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class GenerationRequestRepositoryAdapter implements GenerationRequestRepository {

    private final GenerationRequestJpaRepository jpaRepository;
    private final GenerationRequestMapper mapper;

    public GenerationRequestRepositoryAdapter(GenerationRequestJpaRepository jpaRepository,
                                              GenerationRequestMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public GenerationRequest save(GenerationRequest generationRequest) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(generationRequest)));
    }

    @Override
    public Optional<GenerationRequest> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<GenerationRequest> findByProjectId(UUID projectId) {
        return jpaRepository.findByProjectId(projectId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<GenerationRequest> findByStatus(GenerationRequestStatus status) {
        return jpaRepository.findByStatus(status.name()).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
