package com.guitargpt.infrastructure.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guitargpt.domain.model.GenerationRequest;
import com.guitargpt.domain.model.GenerationRequestStatus;
import com.guitargpt.domain.port.in.GenerationRequestUseCase;
import com.guitargpt.infrastructure.web.dto.request.CreateGenerationRequestRequest;
import com.guitargpt.infrastructure.web.dto.request.UpdateGenerationRequestRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GenerationRequestController.class)
class GenerationRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GenerationRequestUseCase generationRequestUseCase;

    private GenerationRequest createTestRequest() {
        return GenerationRequest.builder()
                .id(UUID.randomUUID())
                .projectId(UUID.randomUUID())
                .promptTemplateId(UUID.randomUUID())
                .userPrompt("Create a blues solo")
                .status(GenerationRequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void create_shouldReturn201() throws Exception {
        GenerationRequest request = createTestRequest();
        when(generationRequestUseCase.create(any(UUID.class), any(UUID.class), any(GenerationRequest.class)))
                .thenReturn(request);

        mockMvc.perform(post("/api/v1/projects/{projectId}/generation-requests", request.getProjectId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateGenerationRequestRequest(request.getPromptTemplateId(), "Create a blues solo"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userPrompt").value("Create a blues solo"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void findByProjectId_shouldReturn200() throws Exception {
        GenerationRequest request = createTestRequest();
        when(generationRequestUseCase.findByProjectId(request.getProjectId())).thenReturn(List.of(request));

        mockMvc.perform(get("/api/v1/projects/{projectId}/generation-requests", request.getProjectId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userPrompt").value("Create a blues solo"));
    }

    @Test
    void findById_shouldReturn200() throws Exception {
        GenerationRequest request = createTestRequest();
        when(generationRequestUseCase.findById(request.getId())).thenReturn(request);

        mockMvc.perform(get("/api/v1/generation-requests/{id}", request.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userPrompt").value("Create a blues solo"));
    }

    @Test
    void findByStatus_shouldReturn200() throws Exception {
        GenerationRequest request = createTestRequest();
        when(generationRequestUseCase.findByStatus(GenerationRequestStatus.PENDING)).thenReturn(List.of(request));

        mockMvc.perform(get("/api/v1/generation-requests/status/{status}", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void update_shouldReturn200() throws Exception {
        GenerationRequest request = createTestRequest();
        request.setStatus(GenerationRequestStatus.COMPLETED);
        request.setResultText("Generated content");
        when(generationRequestUseCase.update(eq(request.getId()), any(GenerationRequest.class))).thenReturn(request);

        mockMvc.perform(put("/api/v1/generation-requests/{id}", request.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateGenerationRequestRequest(GenerationRequestStatus.COMPLETED, "Generated content", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/v1/generation-requests/{id}", UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }

    @Test
    void create_shouldReturn400WhenPromptBlank() throws Exception {
        mockMvc.perform(post("/api/v1/projects/{projectId}/generation-requests", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateGenerationRequestRequest(UUID.randomUUID(), ""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn400WhenTemplateIdNull() throws Exception {
        mockMvc.perform(post("/api/v1/projects/{projectId}/generation-requests", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userPrompt\": \"test\"}"))
                .andExpect(status().isBadRequest());
    }
}
