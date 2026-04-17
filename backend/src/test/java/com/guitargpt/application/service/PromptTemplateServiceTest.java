package com.guitargpt.application.service;

import com.guitargpt.domain.exception.ResourceNotFoundException;
import com.guitargpt.domain.model.PromptTemplate;
import com.guitargpt.domain.model.PromptTemplateCategory;
import com.guitargpt.domain.port.out.PromptTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromptTemplateServiceTest {

    @Mock
    private PromptTemplateRepository promptTemplateRepository;

    @InjectMocks
    private PromptTemplateService service;

    private UUID templateId;
    private PromptTemplate template;

    @BeforeEach
    void setUp() {
        templateId = UUID.randomUUID();
        template = PromptTemplate.builder()
                .name("Blues Solo")
                .description("Generate a blues solo")
                .templateText("Create a {style} solo in {key}")
                .category(PromptTemplateCategory.SOLO)
                .build();
    }

    @Test
    void create_shouldReturnCreatedTemplate() {
        when(promptTemplateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PromptTemplate result = service.create(template);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getName()).isEqualTo("Blues Solo");
        assertThat(result.getTemplateText()).isEqualTo("Create a {style} solo in {key}");
        assertThat(result.getCategory()).isEqualTo(PromptTemplateCategory.SOLO);
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
    }

    @Test
    void findById_shouldReturnTemplate() {
        template.setId(templateId);
        when(promptTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));

        PromptTemplate result = service.findById(templateId);

        assertThat(result.getId()).isEqualTo(templateId);
    }

    @Test
    void findById_shouldThrowWhenNotFound() {
        when(promptTemplateRepository.findById(templateId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(templateId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("PromptTemplate");
    }

    @Test
    void findAll_shouldReturnAllTemplates() {
        when(promptTemplateRepository.findAll()).thenReturn(List.of(template));

        List<PromptTemplate> result = service.findAll();

        assertThat(result).hasSize(1);
    }

    @Test
    void findByCategory_shouldReturnFilteredTemplates() {
        when(promptTemplateRepository.findByCategory(PromptTemplateCategory.SOLO)).thenReturn(List.of(template));

        List<PromptTemplate> result = service.findByCategory(PromptTemplateCategory.SOLO);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategory()).isEqualTo(PromptTemplateCategory.SOLO);
    }

    @Test
    void update_shouldReturnUpdatedTemplate() {
        PromptTemplate existing = PromptTemplate.builder()
                .id(templateId)
                .name("Old Name")
                .templateText("Old text")
                .category(PromptTemplateCategory.PRACTICE)
                .build();

        when(promptTemplateRepository.findById(templateId)).thenReturn(Optional.of(existing));
        when(promptTemplateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PromptTemplate result = service.update(templateId, template);

        assertThat(result.getName()).isEqualTo("Blues Solo");
        assertThat(result.getTemplateText()).isEqualTo("Create a {style} solo in {key}");
        assertThat(result.getCategory()).isEqualTo(PromptTemplateCategory.SOLO);
    }

    @Test
    void update_shouldThrowWhenNotFound() {
        when(promptTemplateRepository.findById(templateId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(templateId, template))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_shouldDeleteExistingTemplate() {
        template.setId(templateId);
        when(promptTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));

        service.delete(templateId);

        verify(promptTemplateRepository).deleteById(templateId);
    }
}
