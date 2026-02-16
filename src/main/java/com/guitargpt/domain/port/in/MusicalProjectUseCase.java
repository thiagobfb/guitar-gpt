package com.guitargpt.domain.port.in;

import com.guitargpt.domain.model.MusicalProject;

import java.util.List;
import java.util.UUID;

public interface MusicalProjectUseCase {

    MusicalProject create(UUID userId, MusicalProject project);

    MusicalProject findById(UUID id);

    List<MusicalProject> findByUserId(UUID userId);

    MusicalProject update(UUID id, MusicalProject project);

    void delete(UUID id);
}
