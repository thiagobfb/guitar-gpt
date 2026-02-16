# Gemini Project Context: GuitarGPT

This document provides a comprehensive overview of the GuitarGPT backend project to be used as a context for AI-assisted development.

## 1. Project Overview

**GuitarGPT** is the backend service for an AI-assisted guitar practice and composition platform. It's a Java-based application built with **Spring Boot 3** and **Java 21**.

The core purpose of the application is to manage:
- **Users**: Musicians on the platform.
- **Musical Projects**: Collections of tracks and ideas.
- **Tracks**: Individual audio stems (e.g., guitar, bass, drums).
- **Prompt Templates**: Reusable templates for generating AI content.
- **Generation Requests**: Asynchronous requests to an AI model to generate musical content like tablatures or practice routines.

### Architecture

The project is designed with a **Hexagonal (Ports & Adapters)** architecture and is **event-driven**. This is a key principle of the codebase.

- **Domain (`src/main/java/com/guitargpt/domain`)**: Contains the core business logic and models. It is pure Java with no framework dependencies.
- **Application (`src/main/java/com/guitargpt/application`)**: Orchestrates the domain logic and implements the use cases (inbound ports).
- **Infrastructure (`src/main/java/com/guitargpt/infrastructure`)**: Contains all the framework-specific code (adapters) for web (REST), persistence (JPA), and messaging (Kafka).

The primary architectural goal is to keep the domain and application layers isolated from external technologies, allowing for better testability and maintainability. A detailed guide can be found in `docs/architecture.md`.

### Technologies
- **Backend**: Java 21, Spring Boot 3
- **Database**: PostgreSQL with schema management by **Flyway**.
- **Messaging**: **Apache Kafka** for asynchronous event handling. The `docker-compose.yml` uses a Redpanda image as a Kafka-compatible broker.
- **API Documentation**: **Springdoc OpenAPI** (accessible at `/swagger-ui.html`).
- **Build Tool**: Apache Maven.

## 2. Building and Running

### With Docker (Recommended)

The easiest way to run the entire stack (application, database, and message broker) is by using Docker Compose.

```bash
# Build and start all services in the background
docker-compose up --build -d
```
This command will:
1. Build the Docker image for the Spring Boot application.
2. Start containers for the application, a PostgreSQL database, and a Redpanda message broker.
3. The application will be available on `http://localhost:8080`.

To stop the services:
```bash
docker-compose down
```

### With Local Maven

If you prefer to run the application directly on your host machine, you will need:
- Java 21
- Apache Maven
- A running PostgreSQL instance
- A running Kafka instance

1.  **Configure the application**: Edit `src/main/resources/application.yml` to match your local database and Kafka connection details.
2.  **Build the project**:
    ```bash
    ./mvnw clean install
    ```
3.  **Run the application**:
    ```bash
    ./mvnw spring-boot:run
    ```

## 3. Testing

To run the full suite of unit and integration tests, use the following Maven command:
```bash
./mvnw test
```
The tests are designed to run without needing a live database or message broker, as they use an in-memory H2 database and mock the external-facing adapters (ports).

## 4. Development Conventions

### Hexagonal Architecture

- **Dependency Rule**: Dependencies always point inwards: `Infrastructure` -> `Application` -> `Domain`. The `domain` layer must not depend on any framework.
- **Ports (Interfaces)**: Located in `domain/port`.
    - `in`: Defines use cases implemented by the `application` layer.
    - `out`: Defines interfaces for external services (like databases or message queues) implemented by the `infrastructure` layer.
- **Adapters (Implementations)**: Located in the `infrastructure` layer. They "adapt" external technologies to the application's ports.

### Database

- **Schema Management**: The database schema is managed exclusively by **Flyway**. Migration scripts are located in `src/main/resources/db/migration`.
- **JPA Entities**: JPA entities (`@Entity`) are considered an infrastructure detail and are located in `infrastructure/persistence/entity`. They are kept separate from the `domain` models.
- **Mappings**: Mappers in `infrastructure/persistence/mapper` are responsible for converting between domain models and JPA entities.

### Asynchronous Operations

- For long-running tasks like AI content generation, the system uses an event-driven flow with Kafka.
- **Flow**:
    1. A client sends a `POST` request to create a `GenerationRequest`.
    2. The application saves the request with a `PENDING` status.
    3. An event is published to a Kafka topic.
    4. The API responds immediately to the client.
    5. A separate consumer (in a real scenario, potentially a different microservice) processes the event, performs the AI generation, and updates the request status via the API.

### Code Style

- **Immutability**: Java `record`s are used for DTOs in the web layer (`infrastructure/web/dto`) to promote immutability.
- **Boilerplate Reduction**: **Lombok** is used to reduce boilerplate code in domain models and JPA entities (e.g., `@Data`, `@NoArgsConstructor`).
