# Repository Guidelines

## Project Structure & Module Organization
- Source: `src/main/java/com/guitargpt/` split into `domain/`, `application/`, and `infrastructure/` (hexagonal architecture).
- Resources: `src/main/resources/` with `application.yml` and Flyway migrations in `db/migration/`.
- Tests: `src/test/java/com/guitargpt/` (service and controller tests) and `src/test/resources/`.
- Docs: `docs/` with architecture notes and ADRs.

## Build, Test, and Development Commands
- `./mvnw clean verify` builds and runs the full test suite.
- `./mvnw test` runs all tests.
- `./mvnw test -pl . -Dtest=UserServiceTest` runs one test class.
- `./mvnw test -pl . -Dtest=UserServiceTest#create_*` runs specific test methods.
- `docker-compose up -d` starts PostgreSQL 16, Redpanda (Kafka), and the app.

## Coding Style & Naming Conventions
- Java 21, Spring Boot 3.x.
- Lombok usage:
  - Domain models: `@Getter @Setter @NoArgsConstructor @AllArgsConstructor`.
  - JPA entities: `@Getter @Setter @NoArgsConstructor` (never use `@Data`).
- JPA entities store FK fields as `UUID` columns (no `@ManyToOne`).
- Enums persisted as `String` (`.name()` / `valueOf()`), null-check when needed.
- Flyway migrations: `V{n}__{description}.sql` in `src/main/resources/db/migration/`.

## Testing Guidelines
- Service tests: Mockito (`@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`) with AssertJ assertions.
- Controller tests: `@WebMvcTest`, `@MockitoBean`, MockMvc for HTTP assertions.
- Tests use H2 in-memory DB; Docker is not required for tests.

## Commit & Pull Request Guidelines
- Commit messages in history are short, imperative summaries (no enforced prefix). Keep them concise and task-focused.
- PRs should include:
  - A clear description of changes and rationale.
  - Test evidence (commands run and results).
  - Any API changes or migration notes.
  - Screenshots are not required (backend-only).

## Architecture Notes
- Follow the ports & adapters flow: domain → application → infrastructure.
- Entity creation checklist: domain model → JPA entity → mapper → repository → adapter → service → controller → DTOs → migration → tests.
- `GenerationRequestService.create()` persists then publishes a Kafka event to topic `generation-requests`.
