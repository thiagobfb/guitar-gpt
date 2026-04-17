package com.guitargpt.infrastructure.web.controller;

import com.guitargpt.domain.model.MusicalProject;
import com.guitargpt.domain.port.in.MusicalProjectUseCase;
import com.guitargpt.infrastructure.web.dto.request.CreateMusicalProjectRequest;
import com.guitargpt.infrastructure.web.dto.request.UpdateMusicalProjectRequest;
import com.guitargpt.infrastructure.web.dto.response.MusicalProjectResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class MusicalProjectController {

    private final MusicalProjectUseCase projectUseCase;

    public MusicalProjectController(MusicalProjectUseCase projectUseCase) {
        this.projectUseCase = projectUseCase;
    }

    @PostMapping("/users/{userId}/projects")
    public ResponseEntity<MusicalProjectResponse> create(
            @PathVariable UUID userId,
            @Valid @RequestBody CreateMusicalProjectRequest request) {
        MusicalProject project = new MusicalProject();
        project.setName(request.name());
        project.setDescription(request.description());
        MusicalProject created = projectUseCase.create(userId, project);
        return ResponseEntity.status(HttpStatus.CREATED).body(MusicalProjectResponse.from(created));
    }

    @GetMapping("/users/{userId}/projects")
    public ResponseEntity<List<MusicalProjectResponse>> findByUserId(@PathVariable UUID userId) {
        List<MusicalProjectResponse> projects = projectUseCase.findByUserId(userId).stream()
                .map(MusicalProjectResponse::from)
                .toList();
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/projects/{id}")
    public ResponseEntity<MusicalProjectResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(MusicalProjectResponse.from(projectUseCase.findById(id)));
    }

    @PutMapping("/projects/{id}")
    public ResponseEntity<MusicalProjectResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMusicalProjectRequest request) {
        MusicalProject project = new MusicalProject();
        project.setName(request.name());
        project.setDescription(request.description());
        MusicalProject updated = projectUseCase.update(id, project);
        return ResponseEntity.ok(MusicalProjectResponse.from(updated));
    }

    @DeleteMapping("/projects/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        projectUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}
