package com.guitargpt.infrastructure.messaging.consumer;

import com.guitargpt.domain.model.GenerationRequest;
import com.guitargpt.domain.model.GenerationRequestStatus;
import com.guitargpt.domain.port.in.GenerationRequestUseCase;
import com.guitargpt.domain.port.out.TablatureGenerator;
import com.guitargpt.infrastructure.messaging.event.GenerationRequestEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GenerationRequestConsumer {

    private final GenerationRequestUseCase generationRequestUseCase;
    private final TablatureGenerator tablatureGenerator;

    public GenerationRequestConsumer(
            GenerationRequestUseCase generationRequestUseCase,
            TablatureGenerator tablatureGenerator) {
        this.generationRequestUseCase = generationRequestUseCase;
        this.tablatureGenerator = tablatureGenerator;
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

            String resultText = tablatureGenerator.generate(event.userPrompt());

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
}
