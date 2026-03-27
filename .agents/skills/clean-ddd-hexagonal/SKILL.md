---
name: clean-ddd-hexagonal
description: Guia de Clean Architecture + DDD + Hexagonal adaptado ao GuitarGPT. Ative em mudanças de domínio, ports, adapters, ou estrutura de camadas. Referência conceitual para decisões de arquitetura.
---

# Clean Architecture + DDD + Hexagonal — GuitarGPT

Referência conceitual para arquitetura do projeto. Os princípios são universais; a **implementação** segue as convenções já estabelecidas no GuitarGPT (ver CLAUDE.md).

## Quando Usar (e Quando NÃO)

| Usar | Não usar |
|------|----------|
| Mudança de regra de negócio ou invariante | CRUD simples sem lógica |
| Novo aggregate ou entidade com comportamento | Correção de bug pontual |
| Decisão sobre onde colocar código (camada) | Ajustes de configuração/infra |
| Revisão de acoplamento entre camadas | Mudanças cosméticas em DTOs |

> **Princípio**: Comece simples. Evolua complexidade só quando necessário. O GuitarGPT V1 prioriza entrega sobre pureza arquitetural.

## REGRA CRÍTICA: Dependências apontam para dentro

```
Infrastructure → Application → Domain
  (adapters)     (services)     (core)
```

**Violações a detectar:**
- Domain importando Spring, JPA, Kafka
- Controllers chamando repositórios diretamente (pulando services)
- Entidades JPA usadas como domain models (o GuitarGPT usa mappers para separar)

## Estrutura do GuitarGPT (real)

```
src/main/java/com/guitargpt/
├── domain/                          # Core: zero dependências Spring
│   ├── model/                       # POJOs com Lombok (@Getter @Setter @NoArgsConstructor @AllArgsConstructor)
│   │   ├── User.java
│   │   ├── MusicalProject.java
│   │   ├── Track.java
│   │   ├── TrackType.java           # Enum (GUITAR, BASS, DRUMS, VOCAL, BACKING_TRACK)
│   │   ├── PromptTemplate.java
│   │   ├── PromptTemplateCategory.java  # Enum (SOLO, COMPOSITION, PRACTICE, RIFF, ARRANGEMENT)
│   │   ├── GenerationRequest.java
│   │   └── GenerationRequestStatus.java # Enum (PENDING, PROCESSING, COMPLETED, FAILED)
│   ├── port/
│   │   ├── in/                      # DRIVER PORTS (use case interfaces)
│   │   │   ├── UserUseCase.java
│   │   │   ├── MusicalProjectUseCase.java
│   │   │   ├── TrackUseCase.java
│   │   │   ├── PromptTemplateUseCase.java
│   │   │   └── GenerationRequestUseCase.java
│   │   └── out/                     # DRIVEN PORTS (repository + event interfaces)
│   │       ├── UserRepository.java
│   │       ├── MusicalProjectRepository.java
│   │       ├── TrackRepository.java
│   │       ├── PromptTemplateRepository.java
│   │       ├── GenerationRequestRepository.java
│   │       └── GenerationRequestEventPublisher.java
│   └── exception/                   # Domain exceptions
│       ├── DomainException.java
│       ├── BusinessRuleException.java
│       └── ResourceNotFoundException.java
│
├── application/                     # Orquestração: @Service @Transactional
│   └── service/
│       ├── UserService.java         # implements UserUseCase
│       ├── MusicalProjectService.java
│       ├── TrackService.java
│       ├── PromptTemplateService.java
│       └── GenerationRequestService.java
│
└── infrastructure/                  # Adapters: tudo que é framework
    ├── web/
    │   ├── controller/              # DRIVER ADAPTERS (REST)
    │   │   ├── UserController.java
    │   │   ├── MusicalProjectController.java
    │   │   ├── TrackController.java
    │   │   ├── PromptTemplateController.java
    │   │   └── GenerationRequestController.java
    │   └── dto/
    │       ├── request/             # Java records com Jakarta validation
    │       └── response/            # Java records
    ├── persistence/
    │   ├── entity/                  # JPA entities (NÃO são domain models)
    │   ├── mapper/                  # Domain ↔ JPA entity (bidirectional)
    │   ├── adapter/                 # DRIVEN ADAPTERS (implements domain ports)
    │   └── repository/              # Spring Data JPA interfaces
    ├── messaging/                   # Kafka adapter (DRIVEN)
    │   ├── publisher/               # Event publishing (Redpanda)
    │   └── consumer/                # Event consuming
    └── config/                      # Spring configs (Security, OpenAPI, etc.)
```

### Mapeamento Conceitual → GuitarGPT

| Conceito DDD/Hexagonal | Implementação no GuitarGPT |
|---|---|
| **Aggregate Root** | Domain model (ex: `MusicalProject`) |
| **Entity** | Domain model com identity UUID |
| **Value Object** | Enums (`TrackType`, `PromptTemplateCategory`) — VOs ricos não usados na V1 |
| **Driver Port** | `domain/port/in/XxxUseCase.java` |
| **Driven Port** | `domain/port/out/XxxRepository.java` |
| **Driver Adapter** | `infrastructure/web/controller/XxxController.java` |
| **Driven Adapter** | `infrastructure/persistence/adapter/XxxRepositoryAdapter.java` |
| **Application Service** | `application/service/XxxService.java` (@Service @Transactional) |
| **Domain Event** | `GenerationRequestEvent` (publicado no Kafka) |
| **Repository** | Interface no domain, implementação no infrastructure |
| **DTO** | Java records em `infrastructure/web/dto/` |
| **Mapper** | Bidirectional em `infrastructure/persistence/mapper/` |

## Decisões Arquiteturais Específicas do GuitarGPT

### 1. Domain models são POJOs, não Rich Domain Models
```java
// GuitarGPT usa Lombok POJOs (anemic por escolha na V1)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Track {
    private UUID id;
    private UUID projectId;
    private String name;
    private TrackType type;
    private String description;
}
```
**Por quê**: V1 prioriza entrega. Comportamento vive nos services. Migrar para Rich Domain quando complexidade justificar.

### 2. Sem camada Presentation separada
Controllers ficam em `infrastructure/web/` (não em `presentation/`). Motivo: Spring Boot trata controllers como adapters de infraestrutura, e o projeto é API-only (sem UI server-side).

### 3. FKs como UUID, sem @ManyToOne
```java
// JPA entity armazena FK como UUID column
private UUID projectId;  // NÃO @ManyToOne(Project.class)
```
**Por quê**: Evita lazy loading surpresa, simplifica testes, e permite referência cross-aggregate por ID (padrão DDD).

### 4. ID gerado na aplicação (UUID.randomUUID())
```java
// No service, ANTES do save
request.setId(UUID.randomUUID());
repository.save(request);
eventPublisher.publish(new GenerationRequestEvent(request.getId()));
```
**Por quê**: ADR-007. Permite publicar evento Kafka com ID antes de persistir. Sem dependência de sequence do DB.

### 5. Enums no domain, String no JPA
```java
// Domain: Java enum
private TrackType type;

// JPA entity: String column
private String type;

// Mapper: valueOf() ↔ .name()
domain.setType(TrackType.valueOf(entity.getType()));
entity.setType(domain.getType().name());
```

## Anti-Patterns a Evitar no GuitarGPT

| Anti-Pattern | Problema | Como Detectar |
|---|---|---|
| **Controller → Repository direto** | Pula use case, acopla HTTP ao banco | Controller importando `XxxRepository` |
| **Domain importando Spring** | Core depende de framework | `import org.springframework` em `domain/` |
| **JPA entity como domain model** | Acoplamento Hibernate no domínio | `@Entity` em `domain/model/` |
| **DTO na camada de serviço** | Application depende de infraestrutura | Service recebendo `CreateXxxRequest` |
| **Lógica de negócio no controller** | Controller faz mais que traduzir HTTP | if/else com regras de negócio no controller |
| **Cross-aggregate transaction** | Duas entidades raiz na mesma TX | Service salvando Project + Track no mesmo método sem evento |

## Testes por Camada

| Camada | Tipo | Framework | Padrão |
|---|---|---|---|
| **Domain** | Unit | JUnit + AssertJ | Sem mocks (domínio é puro) |
| **Application (Service)** | Unit | `@ExtendWith(MockitoExtension.class)` | `@Mock` repositories, `@InjectMocks` service |
| **Infrastructure (Controller)** | Slice | `@WebMvcTest` + `@MockitoBean` | MockMvc para HTTP assertions |
| **Infrastructure (Repository)** | Integration | Testcontainers (quando necessário) | H2 in-memory para testes rápidos |

```java
// Service test (padrão do projeto)
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock private UserRepository userRepository;
    @InjectMocks private UserService userService;

    @Test
    void create_shouldSaveAndReturn() {
        // Arrange, Act, Assert com AssertJ
    }
}

// Controller test (padrão do projeto)
@WebMvcTest(UserController.class)
@Import(TestSecurityConfig.class)
class UserControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private UserUseCase userUseCase;
}
```

## Checklist para Novas Entidades

> Checklist completo em CLAUDE.md: "Entity creation checklist"

1. Domain model (`domain/model/`) → POJO com Lombok
2. JPA entity (`infrastructure/persistence/entity/`) → Lombok + JPA annotations
3. Mapper (`infrastructure/persistence/mapper/`) → bidirectional, null-safe para enums
4. JPA repository (`infrastructure/persistence/repository/`) → Spring Data interface
5. Repository adapter (`infrastructure/persistence/adapter/`) → implements domain port
6. Port in (`domain/port/in/`) → use case interface
7. Port out (`domain/port/out/`) → repository interface
8. Service (`application/service/`) → @Service @Transactional, implements use case
9. Controller (`infrastructure/web/controller/`) → REST, usa use case (não repository)
10. DTOs (`infrastructure/web/dto/`) → Java records com Jakarta validation
11. Flyway migration (`db/migration/`) → `V{n}__{description}.sql`
12. Testes: Service test + Controller test

## Referências Conceituais

Os arquivos em `references/` são **material de referência teórico** (language-agnostic). Para aplicação no GuitarGPT, sempre consulte este SKILL.md e o CLAUDE.md primeiro.

| Arquivo | Uso |
|---|---|
| [references/LAYERS.md](references/LAYERS.md) | Teoria das camadas (adaptar para estrutura do projeto) |
| [references/DDD-STRATEGIC.md](references/DDD-STRATEGIC.md) | Bounded contexts (futuro: quando escalar) |
| [references/DDD-TACTICAL.md](references/DDD-TACTICAL.md) | Entities, Value Objects, Aggregates (teoria) |
| [references/HEXAGONAL.md](references/HEXAGONAL.md) | Ports & Adapters (teoria) |
| [references/CQRS-EVENTS.md](references/CQRS-EVENTS.md) | CQRS e eventos (futuro: quando complexidade justificar) |
| [references/TESTING.md](references/TESTING.md) | Patterns de teste (adaptar para JUnit/Mockito) |
| [references/CHEATSHEET.md](references/CHEATSHEET.md) | Quick reference |

## Evolução Futura (quando justificar)

| De (V1) | Para (V2+) | Trigger |
|---|---|---|
| Anemic domain models | Rich domain models com comportamento | Lógica de negócio complexa nos services |
| Primitivos (UUID, String) | Value Objects (`TrackId`, `ProjectName`) | Type safety necessária |
| Flat `model/` | Organização por aggregate | > 10 entidades no domínio |
| Sem CQRS | Command/Query separation | Read models diferentes dos write models |
| Sem event sourcing | Event sourcing | Auditoria completa necessária |
