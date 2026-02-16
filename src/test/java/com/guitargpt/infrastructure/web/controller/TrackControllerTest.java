package com.guitargpt.infrastructure.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guitargpt.domain.model.Track;
import com.guitargpt.domain.model.TrackType;
import com.guitargpt.domain.port.in.TrackUseCase;
import com.guitargpt.infrastructure.web.dto.request.CreateTrackRequest;
import com.guitargpt.infrastructure.web.dto.request.UpdateTrackRequest;
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

@WebMvcTest(TrackController.class)
class TrackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TrackUseCase trackUseCase;

    private Track createTestTrack() {
        return Track.builder()
                .id(UUID.randomUUID())
                .projectId(UUID.randomUUID())
                .name("Lead Guitar")
                .type(TrackType.GUITAR)
                .description("Main guitar")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void create_shouldReturn201() throws Exception {
        Track track = createTestTrack();
        when(trackUseCase.create(any(UUID.class), any(Track.class))).thenReturn(track);

        mockMvc.perform(post("/api/v1/projects/{projectId}/tracks", track.getProjectId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateTrackRequest("Lead Guitar", TrackType.GUITAR, "Main guitar"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Lead Guitar"))
                .andExpect(jsonPath("$.type").value("GUITAR"));
    }

    @Test
    void findByProjectId_shouldReturn200() throws Exception {
        Track track = createTestTrack();
        when(trackUseCase.findByProjectId(track.getProjectId())).thenReturn(List.of(track));

        mockMvc.perform(get("/api/v1/projects/{projectId}/tracks", track.getProjectId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Lead Guitar"));
    }

    @Test
    void findById_shouldReturn200() throws Exception {
        Track track = createTestTrack();
        when(trackUseCase.findById(track.getId())).thenReturn(track);

        mockMvc.perform(get("/api/v1/tracks/{id}", track.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Lead Guitar"));
    }

    @Test
    void update_shouldReturn200() throws Exception {
        Track track = createTestTrack();
        when(trackUseCase.update(eq(track.getId()), any(Track.class))).thenReturn(track);

        mockMvc.perform(put("/api/v1/tracks/{id}", track.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateTrackRequest("Lead Guitar", TrackType.GUITAR, "Updated"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Lead Guitar"));
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/v1/tracks/{id}", UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }
}
