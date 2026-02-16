package com.guitargpt.domain.port.out;

import com.guitargpt.domain.model.PromptTemplate;
import com.guitargpt.domain.model.PromptTemplateCategory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PromptTemplateRepository {

    PromptTemplate save(PromptTemplate promptTemplate);

    Optional<PromptTemplate> findById(UUID id);

    List<PromptTemplate> findAll();

    List<PromptTemplate> findByCategory(PromptTemplateCategory category);

    void deleteById(UUID id);
}
