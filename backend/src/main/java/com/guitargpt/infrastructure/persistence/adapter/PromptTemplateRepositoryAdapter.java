package com.guitargpt.infrastructure.persistence.adapter;

import com.guitargpt.domain.model.PromptTemplate;
import com.guitargpt.domain.model.PromptTemplateCategory;
import com.guitargpt.domain.port.out.PromptTemplateRepository;
import com.guitargpt.infrastructure.persistence.mapper.PromptTemplateMapper;
import com.guitargpt.infrastructure.persistence.repository.PromptTemplateJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PromptTemplateRepositoryAdapter implements PromptTemplateRepository {

    private final PromptTemplateJpaRepository jpaRepository;
    private final PromptTemplateMapper mapper;

    public PromptTemplateRepositoryAdapter(PromptTemplateJpaRepository jpaRepository, PromptTemplateMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public PromptTemplate save(PromptTemplate promptTemplate) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(promptTemplate)));
    }

    @Override
    public Optional<PromptTemplate> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<PromptTemplate> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<PromptTemplate> findByCategory(PromptTemplateCategory category) {
        return jpaRepository.findByCategory(category.name()).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
