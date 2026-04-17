package com.guitargpt.application.service;

import com.guitargpt.domain.exception.ResourceNotFoundException;
import com.guitargpt.domain.model.MusicalProject;
import com.guitargpt.domain.model.User;
import com.guitargpt.domain.port.out.MusicalProjectRepository;
import com.guitargpt.domain.port.out.UserRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MusicalProjectServiceTest {

    @Mock
    private MusicalProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MusicalProjectService service;

    private UUID userId;
    private UUID projectId;
    private MusicalProject project;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        project = MusicalProject.builder()
                .name("My Song")
                .description("A blues song")
                .build();
    }

    @Test
    void create_shouldReturnCreatedProject() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));
        when(projectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MusicalProject result = service.create(userId, project);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getName()).isEqualTo("My Song");
    }

    @Test
    void create_shouldThrowWhenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(userId, project))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");
    }

    @Test
    void findById_shouldReturnProject() {
        project.setId(projectId);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        MusicalProject result = service.findById(projectId);

        assertThat(result.getId()).isEqualTo(projectId);
    }

    @Test
    void findById_shouldThrowWhenNotFound() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(projectId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findByUserId_shouldReturnProjects() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));
        when(projectRepository.findByUserId(userId)).thenReturn(List.of(project));

        List<MusicalProject> result = service.findByUserId(userId);

        assertThat(result).hasSize(1);
    }

    @Test
    void update_shouldReturnUpdatedProject() {
        MusicalProject existing = MusicalProject.builder()
                .id(projectId)
                .name("Old")
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(existing));
        when(projectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MusicalProject result = service.update(projectId, project);

        assertThat(result.getName()).isEqualTo("My Song");
    }

    @Test
    void delete_shouldDeleteExistingProject() {
        project.setId(projectId);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        service.delete(projectId);

        verify(projectRepository).deleteById(projectId);
    }
}
