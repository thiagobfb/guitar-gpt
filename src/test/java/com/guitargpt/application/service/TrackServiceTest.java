package com.guitargpt.application.service;

import com.guitargpt.domain.exception.ResourceNotFoundException;
import com.guitargpt.domain.model.MusicalProject;
import com.guitargpt.domain.model.Track;
import com.guitargpt.domain.model.TrackType;
import com.guitargpt.domain.port.out.MusicalProjectRepository;
import com.guitargpt.domain.port.out.TrackRepository;
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
class TrackServiceTest {

    @Mock
    private TrackRepository trackRepository;

    @Mock
    private MusicalProjectRepository projectRepository;

    @InjectMocks
    private TrackService service;

    private UUID projectId;
    private UUID trackId;
    private Track track;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        trackId = UUID.randomUUID();
        track = Track.builder()
                .name("Lead Guitar")
                .type(TrackType.GUITAR)
                .description("Main guitar track")
                .build();
    }

    @Test
    void create_shouldReturnCreatedTrack() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(new MusicalProject()));
        when(trackRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Track result = service.create(projectId, track);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getProjectId()).isEqualTo(projectId);
        assertThat(result.getName()).isEqualTo("Lead Guitar");
        assertThat(result.getType()).isEqualTo(TrackType.GUITAR);
    }

    @Test
    void create_shouldThrowWhenProjectNotFound() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(projectId, track))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("MusicalProject");
    }

    @Test
    void findById_shouldReturnTrack() {
        track.setId(trackId);
        when(trackRepository.findById(trackId)).thenReturn(Optional.of(track));

        Track result = service.findById(trackId);

        assertThat(result.getId()).isEqualTo(trackId);
    }

    @Test
    void findByProjectId_shouldReturnTracks() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(new MusicalProject()));
        when(trackRepository.findByProjectId(projectId)).thenReturn(List.of(track));

        List<Track> result = service.findByProjectId(projectId);

        assertThat(result).hasSize(1);
    }

    @Test
    void update_shouldReturnUpdatedTrack() {
        Track existing = Track.builder()
                .id(trackId)
                .name("Old")
                .type(TrackType.BASS)
                .build();

        when(trackRepository.findById(trackId)).thenReturn(Optional.of(existing));
        when(trackRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Track result = service.update(trackId, track);

        assertThat(result.getName()).isEqualTo("Lead Guitar");
        assertThat(result.getType()).isEqualTo(TrackType.GUITAR);
    }

    @Test
    void delete_shouldDeleteExistingTrack() {
        track.setId(trackId);
        when(trackRepository.findById(trackId)).thenReturn(Optional.of(track));

        service.delete(trackId);

        verify(trackRepository).deleteById(trackId);
    }
}
