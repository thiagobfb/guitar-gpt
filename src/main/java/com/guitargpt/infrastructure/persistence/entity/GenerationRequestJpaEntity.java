package com.guitargpt.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "generation_requests")
public class GenerationRequestJpaEntity {

    @Id
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "prompt_template_id", nullable = false)
    private UUID promptTemplateId;

    @Column(name = "user_prompt", nullable = false)
    private String userPrompt;

    @Column(nullable = false)
    private String status;

    @Column(name = "result_text")
    private String resultText;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
