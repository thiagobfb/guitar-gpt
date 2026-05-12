package com.guitargpt.infrastructure.messaging.consumer;

import com.guitargpt.domain.model.GenerationRequest;
import com.guitargpt.domain.model.GenerationRequestStatus;
import com.guitargpt.domain.port.in.GenerationRequestUseCase;
import com.guitargpt.domain.port.out.TablatureGenerator;
import com.guitargpt.infrastructure.messaging.event.GenerationRequestEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenerationRequestConsumerTest {

    private static final String GENERATED_TAB = "e|---0---1---0---|\nB|---1---1---1---|";

    @Mock
    private GenerationRequestUseCase generationRequestUseCase;

    @Mock
    private TablatureGenerator tablatureGenerator;

    @InjectMocks
    private GenerationRequestConsumer consumer;

    private UUID requestId;
    private GenerationRequestEvent event;

    @BeforeEach
    void setUp() {
        requestId = UUID.randomUUID();
        event = new GenerationRequestEvent(
                requestId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Create a blues solo in A minor",
                "PENDING",
                LocalDateTime.now()
        );
    }

    @Test
    void consume_shouldProcessPendingRequest() {
        GenerationRequest pendingRequest = GenerationRequest.builder()
                .id(requestId)
                .status(GenerationRequestStatus.PENDING)
                .build();
        when(generationRequestUseCase.findById(requestId)).thenReturn(pendingRequest);
        when(generationRequestUseCase.update(eq(requestId), any())).thenAnswer(inv -> inv.getArgument(1));
        when(tablatureGenerator.generate(event.userPrompt())).thenReturn(GENERATED_TAB);

        consumer.consume(event);

        ArgumentCaptor<GenerationRequest> captor = ArgumentCaptor.forClass(GenerationRequest.class);
        verify(generationRequestUseCase, times(2)).update(eq(requestId), captor.capture());
        verify(tablatureGenerator).generate(event.userPrompt());

        GenerationRequest processingUpdate = captor.getAllValues().get(0);
        assertThat(processingUpdate.getStatus()).isEqualTo(GenerationRequestStatus.PROCESSING);

        GenerationRequest completedUpdate = captor.getAllValues().get(1);
        assertThat(completedUpdate.getStatus()).isEqualTo(GenerationRequestStatus.COMPLETED);
        assertThat(completedUpdate.getResultText()).isEqualTo(GENERATED_TAB);
    }

    @Test
    void consume_shouldSkipNonPendingRequest() {
        GenerationRequest completedRequest = GenerationRequest.builder()
                .id(requestId)
                .status(GenerationRequestStatus.COMPLETED)
                .build();
        when(generationRequestUseCase.findById(requestId)).thenReturn(completedRequest);

        consumer.consume(event);

        verify(generationRequestUseCase, never()).update(any(), any());
        verify(tablatureGenerator, never()).generate(any());
    }

    @Test
    void consume_shouldSetFailedOnGeneratorException() {
        GenerationRequest pendingRequest = GenerationRequest.builder()
                .id(requestId)
                .status(GenerationRequestStatus.PENDING)
                .build();
        when(generationRequestUseCase.findById(requestId)).thenReturn(pendingRequest);
        when(generationRequestUseCase.update(eq(requestId), any())).thenAnswer(inv -> inv.getArgument(1));
        when(tablatureGenerator.generate(event.userPrompt()))
                .thenThrow(new RuntimeException("Anthropic API unavailable"));

        consumer.consume(event);

        ArgumentCaptor<GenerationRequest> captor = ArgumentCaptor.forClass(GenerationRequest.class);
        verify(generationRequestUseCase, times(2)).update(eq(requestId), captor.capture());

        GenerationRequest failedUpdate = captor.getAllValues().get(1);
        assertThat(failedUpdate.getStatus()).isEqualTo(GenerationRequestStatus.FAILED);
        assertThat(failedUpdate.getErrorMessage()).contains("Anthropic API unavailable");
    }
}
