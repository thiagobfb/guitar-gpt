package com.guitargpt.infrastructure.web.controller;

import com.guitargpt.domain.model.PromptTemplate;
import com.guitargpt.domain.model.PromptTemplateCategory;
import com.guitargpt.domain.port.in.PromptTemplateUseCase;
import com.guitargpt.infrastructure.web.dto.request.CreatePromptTemplateRequest;
import com.guitargpt.infrastructure.web.dto.request.UpdatePromptTemplateRequest;
import com.guitargpt.infrastructure.web.dto.response.PromptTemplateResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/prompt-templates")
public class PromptTemplateController {

    private final PromptTemplateUseCase promptTemplateUseCase;

    public PromptTemplateController(PromptTemplateUseCase promptTemplateUseCase) {
        this.promptTemplateUseCase = promptTemplateUseCase;
    }

    @PostMapping
    public ResponseEntity<PromptTemplateResponse> create(@Valid @RequestBody CreatePromptTemplateRequest request) {
        PromptTemplate promptTemplate = new PromptTemplate();
        promptTemplate.setName(request.name());
        promptTemplate.setTemplateText(request.templateText());
        promptTemplate.setDescription(request.description());
        promptTemplate.setCategory(request.category());
        PromptTemplate created = promptTemplateUseCase.create(promptTemplate);
        return ResponseEntity.status(HttpStatus.CREATED).body(PromptTemplateResponse.from(created));
    }

    @GetMapping
    public ResponseEntity<List<PromptTemplateResponse>> findAll() {
        List<PromptTemplateResponse> templates = promptTemplateUseCase.findAll().stream()
                .map(PromptTemplateResponse::from)
                .toList();
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromptTemplateResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(PromptTemplateResponse.from(promptTemplateUseCase.findById(id)));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<PromptTemplateResponse>> findByCategory(@PathVariable PromptTemplateCategory category) {
        List<PromptTemplateResponse> templates = promptTemplateUseCase.findByCategory(category).stream()
                .map(PromptTemplateResponse::from)
                .toList();
        return ResponseEntity.ok(templates);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PromptTemplateResponse> update(@PathVariable UUID id,
                                                          @Valid @RequestBody UpdatePromptTemplateRequest request) {
        PromptTemplate promptTemplate = new PromptTemplate();
        promptTemplate.setName(request.name());
        promptTemplate.setTemplateText(request.templateText());
        promptTemplate.setDescription(request.description());
        promptTemplate.setCategory(request.category());
        PromptTemplate updated = promptTemplateUseCase.update(id, promptTemplate);
        return ResponseEntity.ok(PromptTemplateResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        promptTemplateUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}
