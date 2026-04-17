package com.guitargpt.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromptTemplate {

    private UUID id;
    private String name;
    private String description;
    private String templateText;
    private PromptTemplateCategory category;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
