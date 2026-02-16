package com.guitargpt.application.service;

import com.guitargpt.domain.exception.ResourceNotFoundException;
import com.guitargpt.domain.model.Track;
import com.guitargpt.domain.port.in.TrackUseCase;
import com.guitargpt.domain.port.out.MusicalProjectRepository;
import com.guitargpt.domain.port.out.TrackRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TrackService implements TrackUseCase {

    private final TrackRepository trackRepository;
    private final MusicalProjectRepository projectRepository;

    public TrackService(TrackRepository trackRepository, MusicalProjectRepository projectRepository) {
        this.trackRepository = trackRepository;
        this.projectRepository = projectRepository;
    }

    @Override
    public Track create(UUID projectId, Track track) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("MusicalProject", projectId));
        track.setId(UUID.randomUUID());
        track.setProjectId(projectId);
        track.setCreatedAt(LocalDateTime.now());
        return trackRepository.save(track);
    }

    @Override
    @Transactional(readOnly = true)
    public Track findById(UUID id) {
        return trackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Track", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Track> findByProjectId(UUID projectId) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("MusicalProject", projectId));
        return trackRepository.findByProjectId(projectId);
    }

    @Override
    public Track update(UUID id, Track track) {
        Track existing = findById(id);
        existing.setName(track.getName());
        existing.setType(track.getType());
        existing.setDescription(track.getDescription());
        return trackRepository.save(existing);
    }

    @Override
    public void delete(UUID id) {
        findById(id);
        trackRepository.deleteById(id);
    }
}
