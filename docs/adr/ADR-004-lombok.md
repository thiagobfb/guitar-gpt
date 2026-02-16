# ADR-004: Adoção do Lombok para Redução de Boilerplate

- **Status:** Aprovado
- **Data:** 2026-02-13
- **Contexto:** Marco 1 — PromptTemplate + GenerationRequest + Kafka

## Contexto

O projeto GuitarGPT utiliza arquitetura hexagonal com domain models (POJOs) e JPA entities separados. Cada classe exigia dezenas de linhas de getters, setters e construtores manuais, aumentando o ruído visual e dificultando a manutenção.

Com a adição dos domínios PromptTemplate (7 campos) e GenerationRequest (9 campos), o volume de boilerplate tornaria os arquivos desnecessariamente extensos.

## Decisão

Adotar **Lombok** como dependência `optional` para eliminar boilerplate em duas categorias de classes:

### Domain Models (`domain/model/`)

Anotações utilizadas: `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`

- `@NoArgsConstructor` — necessário para criação via setters nos services e controllers
- `@AllArgsConstructor` — necessário para construção nos mappers (`toDomain`)
- `@Getter`/`@Setter` — acesso padrão aos campos

### JPA Entities (`infrastructure/persistence/entity/`)

Anotações utilizadas: `@Getter`, `@Setter`, `@NoArgsConstructor`

- `@NoArgsConstructor` — obrigatório pelo JPA/Hibernate
- `@Getter`/`@Setter` — acesso nos mappers (`toEntity`/`toDomain`)
- **Sem `@Data`** — evita `equals`/`hashCode` gerados automaticamente que causam problemas com proxies Hibernate
- **Sem `@AllArgsConstructor`** — entidades JPA são construídas via setters nos mappers

### DTOs (`infrastructure/web/dto/`)

Mantidos como **Java Records** — já são imutáveis e concisos por natureza, sem necessidade de Lombok.

## Entidades afetadas

| Camada | Classe | Anotações |
|--------|--------|-----------|
| domain/model | `User` | `@Getter @Setter @NoArgsConstructor @AllArgsConstructor` |
| domain/model | `MusicalProject` | `@Getter @Setter @NoArgsConstructor @AllArgsConstructor` |
| domain/model | `Track` | `@Getter @Setter @NoArgsConstructor @AllArgsConstructor` |
| domain/model | `PromptTemplate` | `@Getter @Setter @NoArgsConstructor @AllArgsConstructor` |
| domain/model | `GenerationRequest` | `@Getter @Setter @NoArgsConstructor @AllArgsConstructor` |
| persistence/entity | `UserJpaEntity` | `@Getter @Setter @NoArgsConstructor` |
| persistence/entity | `MusicalProjectJpaEntity` | `@Getter @Setter @NoArgsConstructor` |
| persistence/entity | `TrackJpaEntity` | `@Getter @Setter @NoArgsConstructor` |
| persistence/entity | `PromptTemplateJpaEntity` | `@Getter @Setter @NoArgsConstructor` |
| persistence/entity | `GenerationRequestJpaEntity` | `@Getter @Setter @NoArgsConstructor` |

## DTOs (Java Records)

Os DTOs já utilizam Java Records, que são imutáveis e auto-geram `toString`, `equals`, `hashCode` e acessores:

| Tipo | Classe | Campos |
|------|--------|--------|
| Request | `CreatePromptTemplateRequest` | name, templateText, description, category |
| Request | `UpdatePromptTemplateRequest` | name, templateText, description, category |
| Response | `PromptTemplateResponse` | id, name, description, templateText, category, createdAt, updatedAt |
| Request | `CreateGenerationRequestRequest` | promptTemplateId, userPrompt |
| Request | `UpdateGenerationRequestRequest` | status, resultText, errorMessage |
| Response | `GenerationRequestResponse` | id, projectId, promptTemplateId, userPrompt, status, resultText, errorMessage, createdAt, updatedAt |
| Event | `GenerationRequestEvent` | id, projectId, promptTemplateId, userPrompt, status, createdAt |

## Consequências

### Positivas

- **Redução de ~60% de linhas** em domain models e JPA entities
- **Menor risco de erro** ao adicionar novos campos (não é necessário atualizar getters/setters manualmente)
- **Consistência** — todas as classes do mesmo tipo usam o mesmo padrão de anotações

### Negativas

- **Dependência de build-time** — requer annotation processor configurado na IDE
- **Código implícito** — navegação para getters/setters requer plugin Lombok na IDE

### Mitigações

- Lombok declarado como `<optional>true</optional>` no POM — não propaga para consumidores
- Spring Boot gerencia a versão do Lombok automaticamente via BOM
