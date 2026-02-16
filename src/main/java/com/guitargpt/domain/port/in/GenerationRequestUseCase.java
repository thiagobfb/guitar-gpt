package com.guitargpt.domain.port.in;

import com.guitargpt.domain.model.GenerationRequest;
import com.guitargpt.domain.model.GenerationRequestStatus;

import java.util.List;
import java.util.UUID;

public interface GenerationRequestUseCase {

    GenerationRequest create(UUID projectId, UUID promptTemplateId, GenerationRequest generationRequest);

    GenerationRequest findById(UUID id);

    List<GenerationRequest> findByProjectId(UUID projectId);

    List<GenerationRequest> findByStatus(GenerationRequestStatus status);

    GenerationRequest update(UUID id, GenerationRequest generationRequest);

    void delete(UUID id);
}
