package com.guitargpt.application.service;

import com.guitargpt.domain.exception.ResourceNotFoundException;
import com.guitargpt.domain.model.PromptTemplate;
import com.guitargpt.domain.model.PromptTemplateCategory;
import com.guitargpt.domain.port.in.PromptTemplateUseCase;
import com.guitargpt.domain.port.out.PromptTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PromptTemplateService implements PromptTemplateUseCase {

    private final PromptTemplateRepository promptTemplateRepository;

    public PromptTemplateService(PromptTemplateRepository promptTemplateRepository) {
        this.promptTemplateRepository = promptTemplateRepository;
    }

    @Override
    public PromptTemplate create(PromptTemplate promptTemplate) {
        promptTemplate.setId(UUID.randomUUID());
        promptTemplate.setCreatedAt(LocalDateTime.now());
        promptTemplate.setUpdatedAt(LocalDateTime.now());
        return promptTemplateRepository.save(promptTemplate);
    }

    @Override
    @Transactional(readOnly = true)
    public PromptTemplate findById(UUID id) {
        return promptTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PromptTemplate", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromptTemplate> findAll() {
        return promptTemplateRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromptTemplate> findByCategory(PromptTemplateCategory category) {
        return promptTemplateRepository.findByCategory(category);
    }

    @Override
    public PromptTemplate update(UUID id, PromptTemplate promptTemplate) {
        PromptTemplate existing = findById(id);
        existing.setName(promptTemplate.getName());
        existing.setDescription(promptTemplate.getDescription());
        existing.setTemplateText(promptTemplate.getTemplateText());
        existing.setCategory(promptTemplate.getCategory());
        existing.setUpdatedAt(LocalDateTime.now());
        return promptTemplateRepository.save(existing);
    }

    @Override
    public void delete(UUID id) {
        findById(id);
        promptTemplateRepository.deleteById(id);
    }
}
