package com.guitargpt.infrastructure.messaging.adapter;

import com.guitargpt.domain.model.GenerationRequest;
import com.guitargpt.domain.port.out.GenerationRequestEventPublisher;
import com.guitargpt.infrastructure.messaging.event.GenerationRequestEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaGenerationRequestEventPublisher implements GenerationRequestEventPublisher {

    private static final String TOPIC = "generation-requests";

    private final KafkaTemplate<String, GenerationRequestEvent> kafkaTemplate;

    public KafkaGenerationRequestEventPublisher(KafkaTemplate<String, GenerationRequestEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(GenerationRequest generationRequest) {
        GenerationRequestEvent event = new GenerationRequestEvent(
                generationRequest.getId(),
                generationRequest.getProjectId(),
                generationRequest.getPromptTemplateId(),
                generationRequest.getUserPrompt(),
                generationRequest.getStatus().name(),
                generationRequest.getCreatedAt()
        );
        kafkaTemplate.send(TOPIC, generationRequest.getId().toString(), event);
    }
}
