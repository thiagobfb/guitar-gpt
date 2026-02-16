package com.guitargpt.domain.port.out;

import com.guitargpt.domain.model.GenerationRequest;
import com.guitargpt.domain.model.GenerationRequestStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GenerationRequestRepository {

    GenerationRequest save(GenerationRequest generationRequest);

    Optional<GenerationRequest> findById(UUID id);

    List<GenerationRequest> findByProjectId(UUID projectId);

    List<GenerationRequest> findByStatus(GenerationRequestStatus status);

    void deleteById(UUID id);
}
