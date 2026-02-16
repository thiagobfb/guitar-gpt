package com.guitargpt.domain.port.in;

import com.guitargpt.domain.model.Track;

import java.util.List;
import java.util.UUID;

public interface TrackUseCase {

    Track create(UUID projectId, Track track);

    Track findById(UUID id);

    List<Track> findByProjectId(UUID projectId);

    Track update(UUID id, Track track);

    void delete(UUID id);
}
