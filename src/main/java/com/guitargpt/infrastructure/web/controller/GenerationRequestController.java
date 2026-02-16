package com.guitargpt.infrastructure.web.controller;

import com.guitargpt.domain.model.GenerationRequest;
import com.guitargpt.domain.model.GenerationRequestStatus;
import com.guitargpt.domain.port.in.GenerationRequestUseCase;
import com.guitargpt.infrastructure.web.dto.request.CreateGenerationRequestRequest;
import com.guitargpt.infrastructure.web.dto.request.UpdateGenerationRequestRequest;
import com.guitargpt.infrastructure.web.dto.response.GenerationRequestResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class GenerationRequestController {

    private final GenerationRequestUseCase generationRequestUseCase;

    public GenerationRequestController(GenerationRequestUseCase generationRequestUseCase) {
        this.generationRequestUseCase = generationRequestUseCase;
    }

    @PostMapping("/projects/{projectId}/generation-requests")
    public ResponseEntity<GenerationRequestResponse> create(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateGenerationRequestRequest request) {
        GenerationRequest generationRequest = new GenerationRequest();
        generationRequest.setUserPrompt(request.userPrompt());
        GenerationRequest created = generationRequestUseCase.create(projectId, request.promptTemplateId(), generationRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(GenerationRequestResponse.from(created));
    }

    @GetMapping("/projects/{projectId}/generation-requests")
    public ResponseEntity<List<GenerationRequestResponse>> findByProjectId(@PathVariable UUID projectId) {
        List<GenerationRequestResponse> requests = generationRequestUseCase.findByProjectId(projectId).stream()
                .map(GenerationRequestResponse::from)
                .toList();
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/generation-requests/{id}")
    public ResponseEntity<GenerationRequestResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(GenerationRequestResponse.from(generationRequestUseCase.findById(id)));
    }

    @GetMapping("/generation-requests/status/{status}")
    public ResponseEntity<List<GenerationRequestResponse>> findByStatus(@PathVariable GenerationRequestStatus status) {
        List<GenerationRequestResponse> requests = generationRequestUseCase.findByStatus(status).stream()
                .map(GenerationRequestResponse::from)
                .toList();
        return ResponseEntity.ok(requests);
    }

    @PutMapping("/generation-requests/{id}")
    public ResponseEntity<GenerationRequestResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateGenerationRequestRequest request) {
        GenerationRequest generationRequest = new GenerationRequest();
        generationRequest.setStatus(request.status());
        generationRequest.setResultText(request.resultText());
        generationRequest.setErrorMessage(request.errorMessage());
        GenerationRequest updated = generationRequestUseCase.update(id, generationRequest);
        return ResponseEntity.ok(GenerationRequestResponse.from(updated));
    }

    @DeleteMapping("/generation-requests/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        generationRequestUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}
