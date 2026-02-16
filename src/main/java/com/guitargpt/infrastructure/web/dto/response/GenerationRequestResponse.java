package com.guitargpt.infrastructure.web.dto.response;

import com.guitargpt.domain.model.GenerationRequest;
import com.guitargpt.domain.model.GenerationRequestStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record GenerationRequestResponse(
        UUID id,
        UUID projectId,
        UUID promptTemplateId,
        String userPrompt,
        GenerationRequestStatus status,
        String resultText,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static GenerationRequestResponse from(GenerationRequest generationRequest) {
        return new GenerationRequestResponse(
                generationRequest.getId(),
                generationRequest.getProjectId(),
                generationRequest.getPromptTemplateId(),
                generationRequest.getUserPrompt(),
                generationRequest.getStatus(),
                generationRequest.getResultText(),
                generationRequest.getErrorMessage(),
                generationRequest.getCreatedAt(),
                generationRequest.getUpdatedAt()
        );
    }
}
