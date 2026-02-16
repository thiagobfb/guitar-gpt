# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
./mvnw clean verify                # Build + run all tests
./mvnw test                        # Run all tests
./mvnw test -pl . -Dtest=UserServiceTest              # Run a single test class
./mvnw test -pl . -Dtest=UserServiceTest#create_*     # Run specific test methods
docker-compose up -d               # Start PostgreSQL 16, Redpanda (Kafka), and app
```

**Test database**: H2 in-memory (no Docker needed for tests).

## Architecture

Hexagonal architecture (ports & adapters) with strict layer isolation:

- **Domain** (`domain/`) — Pure Java, zero Spring dependencies
  - `model/` — POJOs with Lombok (`@Getter @Setter @NoArgsConstructor @AllArgsConstructor`)
  - `port/in/` — Inbound use case interfaces (e.g. `UserUseCase`)
  - `port/out/` — Outbound repository/event interfaces
  - `exception/` — `BusinessRuleException`, `ResourceNotFoundException` (both extend `DomainException`)
- **Application** (`application/service/`) — `@Service @Transactional`, implements use case interfaces
- **Infrastructure** (`infrastructure/`) — All framework adapters
  - `web/controller/` — REST controllers, `web/dto/request/` and `web/dto/response/` (Java records with Jakarta validation)
  - `persistence/entity/` — JPA entities, `persistence/mapper/` — bidirectional mappers, `persistence/adapter/` — repository implementations, `persistence/repository/` — Spring Data JPA interfaces
  - `messaging/` — Kafka event publishing (Redpanda-compatible)

## Key Patterns

**Entity creation checklist**: Domain model → JPA entity → Mapper → JPA repository → Repository adapter → Service → Controller → Request/Response DTOs → Flyway migration → Tests (service + controller).

**Parent-child entities**: Child services validate parent existence before creating. Nested routes for creation/listing (e.g. `/api/v1/projects/{projectId}/tracks`), flat routes for get/update/delete.

**Enum handling**: Domain uses Java enums (`TrackType`, `GenerationRequestStatus`, `PromptTemplateCategory`). JPA stores as `String` column. Mapper uses `valueOf()` (entity→domain) and `.name()` (domain→entity). For nullable enums, null-check before `valueOf()` (see `PromptTemplateMapper`).

**FK design**: JPA entities store foreign keys as `UUID` columns — no `@ManyToOne` relationships.

**ID generation**: `UUID.randomUUID()` set in service layer before save.

**Async flow**: `GenerationRequestService.create()` saves then publishes a `GenerationRequestEvent` to Kafka topic `generation-requests`.

## Lombok Rules

- Version **must** be pinned to `1.18.40` in pom.xml properties (overrides Spring Boot's managed 1.18.36 for Java 25 compatibility)
- **Never use `@Data` on JPA entities** — causes equals/hashCode issues with Hibernate proxies
- JPA entities use: `@Getter @Setter @NoArgsConstructor`
- Domain models use: `@Getter @Setter @NoArgsConstructor @AllArgsConstructor`

## Testing Conventions

- **Service tests**: `@ExtendWith(MockitoExtension.class)`, `@Mock` repositories, `@InjectMocks` service, AssertJ assertions
- **Controller tests**: `@WebMvcTest(XController.class)`, `@MockitoBean` for use cases, MockMvc for HTTP assertions

## Database

- **PostgreSQL 16** with **Flyway** migrations in `src/main/resources/db/migration/`
- Migration naming: `V{n}__{description}.sql`
- Hibernate DDL mode: `validate` (schema managed entirely by Flyway)
- Child table FKs use `ON DELETE CASCADE`
