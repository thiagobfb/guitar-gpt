package com.guitargpt.infrastructure.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guitargpt.domain.exception.ResourceNotFoundException;
import com.guitargpt.domain.model.User;
import com.guitargpt.domain.port.in.UserUseCase;
import com.guitargpt.infrastructure.web.dto.request.CreateUserRequest;
import com.guitargpt.infrastructure.web.dto.request.UpdateUserRequest;
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

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserUseCase userUseCase;

    private User createTestUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .name("John Doe")
                .email("john@example.com")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void create_shouldReturn201() throws Exception {
        User user = createTestUser();
        when(userUseCase.create(any(User.class))).thenReturn(user);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateUserRequest("John Doe", "john@example.com"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    void create_shouldReturn400WhenInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateUserRequest("", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void findAll_shouldReturn200() throws Exception {
        User user = createTestUser();
        when(userUseCase.findAll()).thenReturn(List.of(user));

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("John Doe"));
    }

    @Test
    void findById_shouldReturn200() throws Exception {
        User user = createTestUser();
        when(userUseCase.findById(user.getId())).thenReturn(user);

        mockMvc.perform(get("/api/v1/users/{id}", user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Doe"));
    }

    @Test
    void findById_shouldReturn404WhenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(userUseCase.findById(id)).thenThrow(new ResourceNotFoundException("User", id));

        mockMvc.perform(get("/api/v1/users/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void update_shouldReturn200() throws Exception {
        User user = createTestUser();
        when(userUseCase.update(eq(user.getId()), any(User.class))).thenReturn(user);

        mockMvc.perform(put("/api/v1/users/{id}", user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserRequest("John Doe", "john@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Doe"));
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/users/{id}", id))
                .andExpect(status().isNoContent());
    }
}
