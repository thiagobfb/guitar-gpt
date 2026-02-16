package com.guitargpt.infrastructure.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guitargpt.domain.model.PromptTemplate;
import com.guitargpt.domain.model.PromptTemplateCategory;
import com.guitargpt.domain.port.in.PromptTemplateUseCase;
import com.guitargpt.infrastructure.web.dto.request.CreatePromptTemplateRequest;
import com.guitargpt.infrastructure.web.dto.request.UpdatePromptTemplateRequest;
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

@WebMvcTest(PromptTemplateController.class)
class PromptTemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PromptTemplateUseCase promptTemplateUseCase;

    private PromptTemplate createTestTemplate() {
        return PromptTemplate.builder()
                .id(UUID.randomUUID())
                .name("Blues Solo")
                .description("Generate a blues solo")
                .templateText("Create a {style} solo in {key}")
                .category(PromptTemplateCategory.SOLO)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void create_shouldReturn201() throws Exception {
        PromptTemplate template = createTestTemplate();
        when(promptTemplateUseCase.create(any(PromptTemplate.class))).thenReturn(template);

        mockMvc.perform(post("/api/v1/prompt-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreatePromptTemplateRequest("Blues Solo", "Create a {style} solo in {key}",
                                        "Generate a blues solo", PromptTemplateCategory.SOLO))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Blues Solo"))
                .andExpect(jsonPath("$.category").value("SOLO"));
    }

    @Test
    void findAll_shouldReturn200() throws Exception {
        PromptTemplate template = createTestTemplate();
        when(promptTemplateUseCase.findAll()).thenReturn(List.of(template));

        mockMvc.perform(get("/api/v1/prompt-templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Blues Solo"));
    }

    @Test
    void findById_shouldReturn200() throws Exception {
        PromptTemplate template = createTestTemplate();
        when(promptTemplateUseCase.findById(template.getId())).thenReturn(template);

        mockMvc.perform(get("/api/v1/prompt-templates/{id}", template.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Blues Solo"));
    }

    @Test
    void findByCategory_shouldReturn200() throws Exception {
        PromptTemplate template = createTestTemplate();
        when(promptTemplateUseCase.findByCategory(PromptTemplateCategory.SOLO)).thenReturn(List.of(template));

        mockMvc.perform(get("/api/v1/prompt-templates/category/{category}", "SOLO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("SOLO"));
    }

    @Test
    void update_shouldReturn200() throws Exception {
        PromptTemplate template = createTestTemplate();
        when(promptTemplateUseCase.update(eq(template.getId()), any(PromptTemplate.class))).thenReturn(template);

        mockMvc.perform(put("/api/v1/prompt-templates/{id}", template.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdatePromptTemplateRequest("Blues Solo", "Create a {style} solo in {key}",
                                        "Updated description", PromptTemplateCategory.SOLO))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Blues Solo"));
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/v1/prompt-templates/{id}", UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }

    @Test
    void create_shouldReturn400WhenNameBlank() throws Exception {
        mockMvc.perform(post("/api/v1/prompt-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreatePromptTemplateRequest("", "Some text", null, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn400WhenTemplateTextBlank() throws Exception {
        mockMvc.perform(post("/api/v1/prompt-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreatePromptTemplateRequest("Name", "", null, null))))
                .andExpect(status().isBadRequest());
    }
}
