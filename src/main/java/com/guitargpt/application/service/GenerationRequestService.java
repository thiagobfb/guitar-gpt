package com.guitargpt.application.service;

import com.guitargpt.domain.exception.ResourceNotFoundException;
import com.guitargpt.domain.model.GenerationRequest;
import com.guitargpt.domain.model.GenerationRequestStatus;
import com.guitargpt.domain.port.in.GenerationRequestUseCase;
import com.guitargpt.domain.port.out.GenerationRequestEventPublisher;
import com.guitargpt.domain.port.out.GenerationRequestRepository;
import com.guitargpt.domain.port.out.MusicalProjectRepository;
import com.guitargpt.domain.port.out.PromptTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class GenerationRequestService implements GenerationRequestUseCase {

    private final GenerationRequestRepository generationRequestRepository;
    private final MusicalProjectRepository projectRepository;
    private final PromptTemplateRepository promptTemplateRepository;
    private final GenerationRequestEventPublisher eventPublisher;

    public GenerationRequestService(GenerationRequestRepository generationRequestRepository,
                                    MusicalProjectRepository projectRepository,
                                    PromptTemplateRepository promptTemplateRepository,
                                    GenerationRequestEventPublisher eventPublisher) {
        this.generationRequestRepository = generationRequestRepository;
        this.projectRepository = projectRepository;
        this.promptTemplateRepository = promptTemplateRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public GenerationRequest create(UUID projectId, UUID promptTemplateId, GenerationRequest generationRequest) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("MusicalProject", projectId));
        promptTemplateRepository.findById(promptTemplateId)
                .orElseThrow(() -> new ResourceNotFoundException("PromptTemplate", promptTemplateId));

        generationRequest.setId(UUID.randomUUID());
        generationRequest.setProjectId(projectId);
        generationRequest.setPromptTemplateId(promptTemplateId);
        generationRequest.setStatus(GenerationRequestStatus.PENDING);
        generationRequest.setCreatedAt(LocalDateTime.now());
        generationRequest.setUpdatedAt(LocalDateTime.now());

        GenerationRequest saved = generationRequestRepository.save(generationRequest);
        eventPublisher.publish(saved);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public GenerationRequest findById(UUID id) {
        return generationRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GenerationRequest", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<GenerationRequest> findByProjectId(UUID projectId) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("MusicalProject", projectId));
        return generationRequestRepository.findByProjectId(projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GenerationRequest> findByStatus(GenerationRequestStatus status) {
        return generationRequestRepository.findByStatus(status);
    }

    @Override
    public GenerationRequest update(UUID id, GenerationRequest generationRequest) {
        GenerationRequest existing = findById(id);
        existing.setStatus(generationRequest.getStatus());
        existing.setResultText(generationRequest.getResultText());
        existing.setErrorMessage(generationRequest.getErrorMessage());
        existing.setUpdatedAt(LocalDateTime.now());
        return generationRequestRepository.save(existing);
    }

    @Override
    public void delete(UUID id) {
        findById(id);
        generationRequestRepository.deleteById(id);
    }
}
