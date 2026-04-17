package com.guitargpt.domain.port.out;

import com.guitargpt.domain.model.Track;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrackRepository {

    Track save(Track track);

    Optional<Track> findById(UUID id);

    List<Track> findByProjectId(UUID projectId);

    void deleteById(UUID id);

    long countByProjectId(UUID projectId);
}
