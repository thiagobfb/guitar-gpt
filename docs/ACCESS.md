# Application Access Points

This document provides quick access URLs and connection details for the services within the GuitarGPT application stack when running via `docker-compose`.

## 1. API Documentation (Swagger UI)

Access the interactive API documentation (OpenAPI/Swagger UI) for the GuitarGPT backend.

-   **URL**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

## 2. PostgreSQL Database

Connect to the PostgreSQL database.

-   **Host**: `localhost`
-   **Port**: `5432`
-   **Database Name**: `guitargpt`
-   **Username**: `guitargpt`
-   **Password**: `guitargpt`

You can use any PostgreSQL client (e.g., `psql`, DBeaver, PgAdmin) to connect using these credentials.

## 3. Kafka (Redpanda) Broker

Connect to the Kafka broker (provided by Redpanda in the Docker Compose setup).

-   **Broker Endpoint**: `localhost:9092`

**Note**: This project setup does not include a web-based Kafka console by default. If you need a visual interface to browse Kafka topics and messages, you would need to add a separate Kafka UI tool (like Redpanda Console, Kafka UI, etc.) to your Docker Compose setup or connect via a desktop Kafka client using the broker endpoint above.

## 4. Spring Boot Actuator Endpoints

Monitor and manage the running Spring Boot application using these endpoints.

-   **Health Check**: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
    -   Provides basic application health information.
-   **Application Info**: [http://localhost:8080/actuator/info](http://localhost:8080/actuator/info)
    -   Displays general application information (can be configured via `application.yml`).
