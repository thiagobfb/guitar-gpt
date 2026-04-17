package com.guitargpt.infrastructure.messaging.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record GenerationRequestEvent(
        UUID id,
        UUID projectId,
        UUID promptTemplateId,
        String userPrompt,
        String status,
        LocalDateTime createdAt
) {
}
