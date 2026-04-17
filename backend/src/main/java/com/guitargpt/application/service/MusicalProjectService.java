package com.guitargpt.application.service;

import com.guitargpt.domain.exception.ResourceNotFoundException;
import com.guitargpt.domain.model.MusicalProject;
import com.guitargpt.domain.port.in.MusicalProjectUseCase;
import com.guitargpt.domain.port.out.MusicalProjectRepository;
import com.guitargpt.domain.port.out.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class MusicalProjectService implements MusicalProjectUseCase {

    private final MusicalProjectRepository projectRepository;
    private final UserRepository userRepository;

    public MusicalProjectService(MusicalProjectRepository projectRepository, UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @Override
    public MusicalProject create(UUID userId, MusicalProject project) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        project.setId(UUID.randomUUID());
        project.setUserId(userId);
        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());
        return projectRepository.save(project);
    }

    @Override
    @Transactional(readOnly = true)
    public MusicalProject findById(UUID id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MusicalProject", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MusicalProject> findByUserId(UUID userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return projectRepository.findByUserId(userId);
    }

    @Override
    public MusicalProject update(UUID id, MusicalProject project) {
        MusicalProject existing = findById(id);
        existing.setName(project.getName());
        existing.setDescription(project.getDescription());
        existing.setUpdatedAt(LocalDateTime.now());
        return projectRepository.save(existing);
    }

    @Override
    public void delete(UUID id) {
        findById(id);
        projectRepository.deleteById(id);
    }
}
