package com.guitargpt.application.service;

import com.guitargpt.domain.exception.ResourceNotFoundException;
import com.guitargpt.domain.model.GenerationRequest;
import com.guitargpt.domain.model.GenerationRequestStatus;
import com.guitargpt.domain.model.MusicalProject;
import com.guitargpt.domain.model.PromptTemplate;
import com.guitargpt.domain.port.out.GenerationRequestEventPublisher;
import com.guitargpt.domain.port.out.GenerationRequestRepository;
import com.guitargpt.domain.port.out.MusicalProjectRepository;
import com.guitargpt.domain.port.out.PromptTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerationRequestServiceTest {

    @Mock
    private GenerationRequestRepository generationRequestRepository;

    @Mock
    private MusicalProjectRepository projectRepository;

    @Mock
    private PromptTemplateRepository promptTemplateRepository;

    @Mock
    private GenerationRequestEventPublisher eventPublisher;

    @InjectMocks
    private GenerationRequestService service;

    private UUID projectId;
    private UUID templateId;
    private UUID requestId;
    private GenerationRequest generationRequest;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        templateId = UUID.randomUUID();
        requestId = UUID.randomUUID();
        generationRequest = GenerationRequest.builder()
                .userPrompt("Create a blues solo in A minor")
                .build();
    }

    @Test
    void create_shouldReturnCreatedRequest() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(new MusicalProject()));
        when(promptTemplateRepository.findById(templateId)).thenReturn(Optional.of(new PromptTemplate()));
        when(generationRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GenerationRequest result = service.create(projectId, templateId, generationRequest);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getProjectId()).isEqualTo(projectId);
        assertThat(result.getPromptTemplateId()).isEqualTo(templateId);
        assertThat(result.getStatus()).isEqualTo(GenerationRequestStatus.PENDING);
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
    }

    @Test
    void create_shouldPublishEvent() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(new MusicalProject()));
        when(promptTemplateRepository.findById(templateId)).thenReturn(Optional.of(new PromptTemplate()));
        when(generationRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.create(projectId, templateId, generationRequest);

        verify(eventPublisher).publish(any(GenerationRequest.class));
    }

    @Test
    void create_shouldThrowWhenProjectNotFound() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(projectId, templateId, generationRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("MusicalProject");
    }

    @Test
    void create_shouldThrowWhenTemplateNotFound() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(new MusicalProject()));
        when(promptTemplateRepository.findById(templateId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(projectId, templateId, generationRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("PromptTemplate");
    }

    @Test
    void findById_shouldReturnRequest() {
        generationRequest.setId(requestId);
        when(generationRequestRepository.findById(requestId)).thenReturn(Optional.of(generationRequest));

        GenerationRequest result = service.findById(requestId);

        assertThat(result.getId()).isEqualTo(requestId);
    }

    @Test
    void findById_shouldThrowWhenNotFound() {
        when(generationRequestRepository.findById(requestId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(requestId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("GenerationRequest");
    }

    @Test
    void findByProjectId_shouldReturnRequests() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(new MusicalProject()));
        when(generationRequestRepository.findByProjectId(projectId)).thenReturn(List.of(generationRequest));

        List<GenerationRequest> result = service.findByProjectId(projectId);

        assertThat(result).hasSize(1);
    }

    @Test
    void findByProjectId_shouldThrowWhenProjectNotFound() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByProjectId(projectId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("MusicalProject");
    }

    @Test
    void findByStatus_shouldReturnFilteredRequests() {
        generationRequest.setStatus(GenerationRequestStatus.PENDING);
        when(generationRequestRepository.findByStatus(GenerationRequestStatus.PENDING))
                .thenReturn(List.of(generationRequest));

        List<GenerationRequest> result = service.findByStatus(GenerationRequestStatus.PENDING);

        assertThat(result).hasSize(1);
    }

    @Test
    void update_shouldReturnUpdatedRequest() {
        GenerationRequest existing = GenerationRequest.builder()
                .id(requestId)
                .status(GenerationRequestStatus.PENDING)
                .build();

        GenerationRequest updateData = GenerationRequest.builder()
                .status(GenerationRequestStatus.COMPLETED)
                .resultText("Generated solo content")
                .build();

        when(generationRequestRepository.findById(requestId)).thenReturn(Optional.of(existing));
        when(generationRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GenerationRequest result = service.update(requestId, updateData);

        assertThat(result.getStatus()).isEqualTo(GenerationRequestStatus.COMPLETED);
        assertThat(result.getResultText()).isEqualTo("Generated solo content");
    }

    @Test
    void delete_shouldDeleteExistingRequest() {
        generationRequest.setId(requestId);
        when(generationRequestRepository.findById(requestId)).thenReturn(Optional.of(generationRequest));

        service.delete(requestId);

        verify(generationRequestRepository).deleteById(requestId);
    }
}
