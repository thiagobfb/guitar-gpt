package com.guitargpt.infrastructure.messaging.consumer;

import com.guitargpt.domain.model.GenerationRequest;
import com.guitargpt.domain.model.GenerationRequestStatus;
import com.guitargpt.domain.port.in.GenerationRequestUseCase;
import com.guitargpt.infrastructure.messaging.event.GenerationRequestEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class GenerationRequestConsumer {

    private static final Logger log = LoggerFactory.getLogger(GenerationRequestConsumer.class);

    private final GenerationRequestUseCase generationRequestUseCase;

    public GenerationRequestConsumer(GenerationRequestUseCase generationRequestUseCase) {
        this.generationRequestUseCase = generationRequestUseCase;
    }

    @KafkaListener(topics = "generation-requests", groupId = "guitargpt-group")
    public void consume(GenerationRequestEvent event) {
        log.info("Received generation request event: id={}, status={}", event.id(), event.status());

        GenerationRequest request = generationRequestUseCase.findById(event.id());

        if (request.getStatus() != GenerationRequestStatus.PENDING) {
            log.info("Skipping non-PENDING request: id={}, status={}", event.id(), request.getStatus());
            return;
        }

        try {
            GenerationRequest processing = new GenerationRequest();
            processing.setStatus(GenerationRequestStatus.PROCESSING);
            generationRequestUseCase.update(event.id(), processing);

            String resultText = generateMockTablature(event.userPrompt());

            GenerationRequest completed = new GenerationRequest();
            completed.setStatus(GenerationRequestStatus.COMPLETED);
            completed.setResultText(resultText);
            generationRequestUseCase.update(event.id(), completed);

            log.info("Successfully processed generation request: id={}", event.id());
        } catch (Exception e) {
            log.error("Failed to process generation request: id={}", event.id(), e);

            GenerationRequest failed = new GenerationRequest();
            failed.setStatus(GenerationRequestStatus.FAILED);
            failed.setErrorMessage("Processing failed: " + e.getMessage());
            generationRequestUseCase.update(event.id(), failed);
        }
    }

    private String generateMockTablature(String userPrompt) {
        return """
                GuitarGPT — Generated Tablature
                Prompt: %s

                e|---0---1---0---3---1---0---------|
                B|---1---1---1---0---1---1---------|
                G|---0---2---0---0---2---0---------|
                D|---2---3---2---0---3---2---------|
                A|---3---3---3---2---3---3---------|
                E|---x---1---x---3---1---x---------|
                """.formatted(userPrompt);
    }
}
