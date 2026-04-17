package com.guitargpt.domain.port.in;

import com.guitargpt.domain.model.PromptTemplate;
import com.guitargpt.domain.model.PromptTemplateCategory;

import java.util.List;
import java.util.UUID;

public interface PromptTemplateUseCase {

    PromptTemplate create(PromptTemplate promptTemplate);

    PromptTemplate findById(UUID id);

    List<PromptTemplate> findAll();

    List<PromptTemplate> findByCategory(PromptTemplateCategory category);

    PromptTemplate update(UUID id, PromptTemplate promptTemplate);

    void delete(UUID id);
}
