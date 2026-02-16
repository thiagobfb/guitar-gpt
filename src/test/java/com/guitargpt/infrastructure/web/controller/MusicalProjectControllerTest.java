package com.guitargpt.infrastructure.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guitargpt.domain.exception.ResourceNotFoundException;
import com.guitargpt.domain.model.MusicalProject;
import com.guitargpt.domain.port.in.MusicalProjectUseCase;
import com.guitargpt.infrastructure.web.dto.request.CreateMusicalProjectRequest;
import com.guitargpt.infrastructure.web.dto.request.UpdateMusicalProjectRequest;
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

@WebMvcTest(MusicalProjectController.class)
class MusicalProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MusicalProjectUseCase projectUseCase;

    private MusicalProject createTestProject() {
        return MusicalProject.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .name("Blues Jam")
                .description("A blues jam session")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void create_shouldReturn201() throws Exception {
        MusicalProject project = createTestProject();
        when(projectUseCase.create(any(UUID.class), any(MusicalProject.class))).thenReturn(project);

        mockMvc.perform(post("/api/v1/users/{userId}/projects", project.getUserId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateMusicalProjectRequest("Blues Jam", "A blues jam session"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Blues Jam"));
    }

    @Test
    void findByUserId_shouldReturn200() throws Exception {
        MusicalProject project = createTestProject();
        when(projectUseCase.findByUserId(project.getUserId())).thenReturn(List.of(project));

        mockMvc.perform(get("/api/v1/users/{userId}/projects", project.getUserId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Blues Jam"));
    }

    @Test
    void findById_shouldReturn200() throws Exception {
        MusicalProject project = createTestProject();
        when(projectUseCase.findById(project.getId())).thenReturn(project);

        mockMvc.perform(get("/api/v1/projects/{id}", project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Blues Jam"));
    }

    @Test
    void findById_shouldReturn404WhenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(projectUseCase.findById(id)).thenThrow(new ResourceNotFoundException("MusicalProject", id));

        mockMvc.perform(get("/api/v1/projects/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_shouldReturn200() throws Exception {
        MusicalProject project = createTestProject();
        when(projectUseCase.update(eq(project.getId()), any(MusicalProject.class))).thenReturn(project);

        mockMvc.perform(put("/api/v1/projects/{id}", project.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateMusicalProjectRequest("Blues Jam", "Updated"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Blues Jam"));
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/v1/projects/{id}", UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }
}
