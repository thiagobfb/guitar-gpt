---
name: clean-ddd-hexagonal
description: Clean Architecture + DDD + Hexagonal guide adapted to GuitarGPT. Trigger on domain changes, ports, adapters, or layer structure decisions. Conceptual reference for architecture decisions.
---

# Clean Architecture + DDD + Hexagonal — GuitarGPT

Conceptual reference for project architecture. Principles are universal; the **implementation** follows conventions already established in GuitarGPT (see CLAUDE.md).

## When to Use (and When NOT to)

| Use | Skip |
|-----|------|
| Business rule or invariant change | Simple CRUD with no logic |
| New aggregate or entity with behavior | Punctual bug fix |
| Deciding where code belongs (which layer) | Configuration/infra adjustments |
| Reviewing coupling between layers | Cosmetic DTO changes |

> **Principle**: Start simple. Evolve complexity only when needed. GuitarGPT V1 prioritizes delivery over architectural purity.

## CRITICAL: The Dependency Rule

```
Infrastructure → Application → Domain
  (adapters)     (services)     (core)
```

**Violations to catch:**
- Domain importing Spring, JPA, or Kafka
- Controllers calling repositories directly (bypassing services)
- JPA entities used as domain models (GuitarGPT uses mappers to separate them)

## GuitarGPT Project Structure (actual)

```
src/main/java/com/guitargpt/
├── domain/                          # Core: zero Spring dependencies
│   ├── model/                       # POJOs with Lombok (@Getter @Setter @NoArgsConstructor @AllArgsConstructor)
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
├── application/                     # Orchestration: @Service @Transactional
│   └── service/
│       ├── UserService.java         # implements UserUseCase
│       ├── MusicalProjectService.java
│       ├── TrackService.java
│       ├── PromptTemplateService.java
│       └── GenerationRequestService.java
│
└── infrastructure/                  # Adapters: all framework code
    ├── web/
    │   ├── controller/              # DRIVER ADAPTERS (REST)
    │   │   ├── UserController.java
    │   │   ├── MusicalProjectController.java
    │   │   ├── TrackController.java
    │   │   ├── PromptTemplateController.java
    │   │   └── GenerationRequestController.java
    │   └── dto/
    │       ├── request/             # Java records with Jakarta validation
    │       └── response/            # Java records
    ├── persistence/
    │   ├── entity/                  # JPA entities (NOT domain models)
    │   ├── mapper/                  # Domain <-> JPA entity (bidirectional)
    │   ├── adapter/                 # DRIVEN ADAPTERS (implements domain ports)
    │   └── repository/              # Spring Data JPA interfaces
    ├── messaging/                   # Kafka adapter (DRIVEN)
    │   ├── publisher/               # Event publishing (Redpanda)
    │   └── consumer/                # Event consuming
    └── config/                      # Spring configs (Security, OpenAPI, etc.)
```

### Concept-to-Implementation Mapping

| DDD / Hexagonal Concept | GuitarGPT Implementation |
|---|---|
| **Aggregate Root** | Domain model (e.g. `MusicalProject`) |
| **Entity** | Domain model with UUID identity |
| **Value Object** | Enums (`TrackType`, `PromptTemplateCategory`) — rich VOs not used in V1 |
| **Driver Port** | `domain/port/in/XxxUseCase.java` |
| **Driven Port** | `domain/port/out/XxxRepository.java` |
| **Driver Adapter** | `infrastructure/web/controller/XxxController.java` |
| **Driven Adapter** | `infrastructure/persistence/adapter/XxxRepositoryAdapter.java` |
| **Application Service** | `application/service/XxxService.java` (@Service @Transactional) |
| **Domain Event** | `GenerationRequestEvent` (published to Kafka) |
| **Repository** | Interface in domain, implementation in infrastructure |
| **DTO** | Java records in `infrastructure/web/dto/` |
| **Mapper** | Bidirectional in `infrastructure/persistence/mapper/` |

## GuitarGPT-Specific Architectural Decisions

### 1. Domain models are POJOs, not Rich Domain Models
```java
// GuitarGPT uses Lombok POJOs (anemic by V1 choice)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Track {
    private UUID id;
    private UUID projectId;
    private String name;
    private TrackType type;
    private String description;
}
```
**Why**: V1 prioritizes delivery. Behavior lives in services. Migrate to Rich Domain Models when complexity justifies it.

### 2. No separate Presentation layer
Controllers live in `infrastructure/web/` (not in a separate `presentation/`). Reason: Spring Boot treats controllers as infrastructure adapters, and the project is API-only (no server-side UI).

### 3. FKs as UUID columns, no @ManyToOne
```java
// JPA entity stores FK as UUID column
private UUID projectId;  // NOT @ManyToOne(Project.class)
```
**Why**: Avoids surprise lazy loading, simplifies tests, and allows cross-aggregate reference by ID (DDD pattern).

### 4. ID generated in application layer (UUID.randomUUID())
```java
// In service, BEFORE save
request.setId(UUID.randomUUID());
repository.save(request);
eventPublisher.publish(new GenerationRequestEvent(request.getId()));
```
**Why**: ADR-007. Allows publishing Kafka event with ID before persisting. No DB sequence dependency.

### 5. Enums in domain, String in JPA
```java
// Domain: Java enum
private TrackType type;

// JPA entity: String column
private String type;

// Mapper: valueOf() <-> .name()
domain.setType(TrackType.valueOf(entity.getType()));
entity.setType(domain.getType().name());
```

## Anti-Patterns to Avoid

| Anti-Pattern | Problem | How to Detect |
|---|---|---|
| **Controller -> Repository directly** | Bypasses use case, couples HTTP to DB | Controller importing `XxxRepository` |
| **Domain importing Spring** | Core depends on framework | `import org.springframework` in `domain/` |
| **JPA entity as domain model** | Hibernate coupling in domain | `@Entity` in `domain/model/` |
| **DTO in service layer** | Application depends on infrastructure | Service receiving `CreateXxxRequest` |
| **Business logic in controller** | Controller does more than translate HTTP | if/else with business rules in controller |
| **Cross-aggregate transaction** | Two aggregate roots in same TX | Service saving Project + Track in same method without event |

## Testing by Layer

| Layer | Type | Framework | Pattern |
|---|---|---|---|
| **Domain** | Unit | JUnit + AssertJ | No mocks needed (domain is pure) |
| **Application (Service)** | Unit | `@ExtendWith(MockitoExtension.class)` | `@Mock` repositories, `@InjectMocks` service |
| **Infrastructure (Controller)** | Slice | `@WebMvcTest` + `@MockitoBean` | MockMvc for HTTP assertions |
| **Infrastructure (Repository)** | Integration | Testcontainers (when needed) | H2 in-memory for fast tests |

```java
// Service test (project pattern)
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock private UserRepository userRepository;
    @InjectMocks private UserService userService;

    @Test
    void create_shouldSaveAndReturn() {
        // Arrange, Act, Assert with AssertJ
    }
}

// Controller test (project pattern)
@WebMvcTest(UserController.class)
@Import(TestSecurityConfig.class)
class UserControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private UserUseCase userUseCase;
}
```

## New Entity Checklist

> Full checklist in CLAUDE.md: "Entity creation checklist"

1. Domain model (`domain/model/`) — POJO with Lombok
2. JPA entity (`infrastructure/persistence/entity/`) — Lombok + JPA annotations
3. Mapper (`infrastructure/persistence/mapper/`) — bidirectional, null-safe for enums
4. JPA repository (`infrastructure/persistence/repository/`) — Spring Data interface
5. Repository adapter (`infrastructure/persistence/adapter/`) — implements domain port
6. Port in (`domain/port/in/`) — use case interface
7. Port out (`domain/port/out/`) — repository interface
8. Service (`application/service/`) — @Service @Transactional, implements use case
9. Controller (`infrastructure/web/controller/`) — REST, uses use case (not repository)
10. DTOs (`infrastructure/web/dto/`) — Java records with Jakarta validation
11. Flyway migration (`db/migration/`) — `V{n}__{description}.sql`
12. Tests: Service test + Controller test

## Reference Documentation

Files in `references/` are **theoretical reference material** (language-agnostic). For GuitarGPT application, always consult this SKILL.md and CLAUDE.md first.

| File | Purpose |
|------|---------|
| [references/LAYERS.md](references/LAYERS.md) | Layer theory (adapt to project structure) |
| [references/DDD-STRATEGIC.md](references/DDD-STRATEGIC.md) | Bounded contexts (future: when scaling) |
| [references/DDD-TACTICAL.md](references/DDD-TACTICAL.md) | Entities, Value Objects, Aggregates (theory) |
| [references/HEXAGONAL.md](references/HEXAGONAL.md) | Ports & Adapters (theory) |
| [references/CQRS-EVENTS.md](references/CQRS-EVENTS.md) | CQRS and events (future: when complexity justifies) |
| [references/TESTING.md](references/TESTING.md) | Testing patterns (adapt to JUnit/Mockito) |
| [references/CHEATSHEET.md](references/CHEATSHEET.md) | Quick reference |

## Future Evolution (when justified)

| From (V1) | To (V2+) | Trigger |
|---|---|---|
| Anemic domain models | Rich domain models with behavior | Complex business logic in services |
| Primitives (UUID, String) | Value Objects (`TrackId`, `ProjectName`) | Type safety needed |
| Flat `model/` | Organization by aggregate | > 10 entities in domain |
| No CQRS | Command/Query separation | Read models differ from write models |
| No event sourcing | Event sourcing | Full audit trail required |
