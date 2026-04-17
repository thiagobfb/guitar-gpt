package com.guitargpt.domain.port.out;

import com.guitargpt.domain.model.MusicalProject;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MusicalProjectRepository {

    MusicalProject save(MusicalProject project);

    Optional<MusicalProject> findById(UUID id);

    List<MusicalProject> findByUserId(UUID userId);

    void deleteById(UUID id);
}
