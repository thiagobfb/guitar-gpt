# GuitarGPT – Backend for an AI-Assisted Guitar Practice Platform

GuitarGPT is a backend service designed to support an AI-assisted guitar practice and composition platform.  
It manages users, musical projects, tracks, prompt templates and generation requests (for example, requests to generate tablatures or practice routines using AI).  
The system is built to be event-driven and cloud-ready, with a strong focus on clean architecture, extensibility, and demonstrable patterns for portfolio and technical interviews.

---

## Technologies and Stack Decisions

### Why Java 21 (LTS)

The project was built with **Java 21**, which is one of the most recent **LTS (Long Term Support)** versions of the platform. The choice was not just about “using the latest version”, but for practical reasons:

- **Long-term support (LTS)**  
  Java 21 provides stability and long-term support, which makes it a solid foundation for modern enterprise applications.

- **Modern language features**  
  The project leverages features introduced in recent Java versions, such as:
  - `record` to model **DTOs** and **commands** in a concise, immutable and expressive way;
  - performance and JVM improvements added in newer releases.

- **Alignment with the Spring ecosystem**  
  Spring Boot 3 is designed to run on Java 17+; choosing Java 21 ensures compatibility and allows the project to benefit from the latest platform optimizations.

In summary, Java 21 combines **modernity**, **stability**, and **market alignment**, which is important both for the evolution of the project and for portfolio purposes.

---

### Why Spring Boot 3.x

The backend uses **Spring Boot 3.x**, which is the de facto standard for modern Java applications in the Spring ecosystem. Some points that motivated this choice:

- **Mature ecosystem**  
  Spring Boot 3 is widely adopted in the industry, with a large amount of libraries, documentation and examples. This makes the project closer to what is found in real production environments.

- **Compatibility with modern Java and Jakarta EE**  
  The 3.x line is already aligned with:
  - Java 17+ / 21  
  - the migration to `jakarta.*` (Jakarta EE 9+), required by the latest Spring versions.

- **Simplified integration with the rest of the stack**  
  The project integrates:
  - **Spring Web** (REST APIs),
  - **Spring Data JPA** (persistence with PostgreSQL),
  - **Flyway** (database migrations),
  - **Spring Kafka** (event publishing in Kafka),
  - **Springdoc OpenAPI** (API documentation),
  and Spring Boot 3 makes this integration easier through autoconfiguration and specific starters.

- **Solid base for a hexagonal architecture**  
  The chosen hexagonal architecture clearly separates:
  - **domain** (business rules, ports),
  - **application** (use cases),
  - **infrastructure** (web, persistence, messaging),
  
  and Spring Boot is treated as an **infrastructure detail**, without contaminating the domain. This is well aligned with modern system design best practices in Java.

---

Together, **Java 21 + Spring Boot 3.x** provide a modern, robust foundation consistent with what companies are currently using in production, while also allowing more advanced concepts to be demonstrated, such as:

- hexagonal architecture (ports & adapters),
- use of `record` for modeling DTOs and commands,
- messaging with Kafka,
- containerized infrastructure (API + database + broker).

These choices were made with a focus on **hands-on learning** and **showcasing relevant skills in technical interviews**.

---

## System Overview

GuitarGPT revolves around a few core concepts:

- **User** – represents the musician using the platform.
- **Musical Project** – a project or song idea that groups related tracks and AI interactions.
- **Track** – an audio track or stem (e.g., guitar, backing track, bass, drums) associated with a project.
- **Prompt Template** – reusable templates used to build prompts for AI models (e.g., “generate a solo in the style of…”).
- **Generation Request** – a request to an AI agent to generate content (tablatures, practice routines, harmonic suggestions, etc.).

Typical flow:

1. A user creates a **musical project** and adds **tracks** to it.
2. The user selects or customizes a **prompt template**.
3. The user creates a **generation request** (e.g., “generate a solo over this progression”).
4. The backend stores the request and **publishes an event to Kafka**.
5. A separate worker/service (not part of this repository) can consume the Kafka event, call an AI provider, and update the request status and result.

This separation allows the API to stay responsive while heavier processing happens asynchronously.

---

## Architecture Overview

The system follows a **hexagonal architecture (ports & adapters)** and an **event-driven** approach. At a high level:

- The **domain** contains the core business model and ports (interfaces).
- The **application** layer implements use cases and orchestrates domain logic.
- The **infrastructure** layer contains:
  - web adapters (REST controllers + DTOs),
  - persistence adapters (JPA + PostgreSQL),
  - messaging adapters (Kafka producers).

A simplified view of the main interactions:

flowchart LR
    User[User / Frontend] --> API[REST API (Spring Web)]
    API --> UC[Application Layer<br/>Use Cases]
    UC --> DOM[Domain Model]
    UC --> DB[(PostgreSQL)]
    UC --> KAFKA[(Kafka)]

    subgraph Infra
        API
        DB
        KAFKA
    end

    KAFKA --> AIWORKER[Future AI Worker<br/>(external service)]


----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------


# (PT-Br) GuitarGPT – Backend para uma Plataforma de Prática de Guitarra com IA

GuitarGPT é um serviço de backend projetado para suportar uma plataforma de prática e composição de guitarra assistida por IA.  
Ele gerencia usuários, projetos musicais, trilhas, templates de prompt e requisições de geração (por exemplo, pedidos para gerar tablaturas ou rotinas de estudo usando IA).  
O sistema foi construído para ser orientado a eventos e pronto para nuvem, com foco em arquitetura limpa, extensibilidade e padrões demonstráveis para portfólio e entrevistas técnicas.

---

## Tecnologias e Decisões de Stack

### Por que Java 21 (LTS)

O projeto foi desenvolvido em **Java 21**, que é uma das versões **LTS (Long Term Support)** mais recentes da plataforma. A escolha não foi apenas por “usar a versão mais nova”, mas por motivos práticos:

- **Suporte de longo prazo (LTS)**  
  Java 21 oferece estabilidade e suporte prolongado, o que o torna uma base sólida para aplicações corporativas modernas.

- **Recursos modernos da linguagem**  
  O projeto aproveita funcionalidades introduzidas nas versões mais recentes do Java, como:
  - `record` para modelar **DTOs** e **commands** de forma concisa, imutável e expressiva;
  - melhorias de performance e de JVM presentes nas versões mais novas.

- **Alinhamento com o ecossistema Spring**  
  Spring Boot 3 foi pensado para rodar em Java 17+; optar por Java 21 garante compatibilidade e permite aproveitar otimizações mais recentes da plataforma.

Em resumo, Java 21 combina **modernidade**, **estabilidade** e **aderência ao mercado**, o que é importante tanto para a evolução do projeto quanto para fins de portfólio.

---

### Por que Spring Boot 3.x

O backend utiliza **Spring Boot 3.x**, que hoje é o padrão de fato para aplicações modernas em Java no ecossistema Spring. Alguns pontos que motivaram essa escolha:

- **Ecossistema maduro**  
  Spring Boot 3 é amplamente adotado no mercado, com grande quantidade de bibliotecas, documentação e exemplos. Isso aproxima o projeto da realidade encontrada em ambientes produtivos.

- **Compatibilidade com Java moderno e Jakarta EE**  
  A linha 3.x já está alinhada com:
  - Java 17+ / 21  
  - migração para `jakarta.*` (Jakarta EE 9+), exigida pelas versões mais novas do Spring.

- **Integração simplificada com o restante da stack**  
  O projeto integra:
  - **Spring Web** (APIs REST),
  - **Spring Data JPA** (persistência com PostgreSQL),
  - **Flyway** (migrações de banco),
  - **Spring Kafka** (publicação de eventos em Kafka),
  - **Springdoc OpenAPI** (documentação da API),
  e o Spring Boot 3 facilita essa integração com autoconfiguração e starters específicos.

- **Base sólida para arquitetura hexagonal**  
  A arquitetura hexagonal adotada separa claramente:
  - **domínio** (regras de negócio, ports),
  - **aplicação** (casos de uso),
  - **infraestrutura** (web, persistência, mensageria),
  
  e o Spring Boot é tratado como um **detalhe de infraestrutura**, sem contaminar o domínio. Isso é bem alinhado com as boas práticas modernas de design de sistemas em Java.

---

Em conjunto, **Java 21 + Spring Boot 3.x** oferecem uma base moderna e robusta, consistente com o que empresas usam hoje em produção, ao mesmo tempo em que permitem demonstrar conceitos mais avançados, como:

- arquitetura hexagonal (ports & adapters),
- uso de `record` para modelagem de DTOs e comandos,
- mensageria com Kafka,
- infraestrutura conteinerizada (API + banco + broker).

Essas escolhas foram feitas com foco em **aprendizado prático** e **exposição de conhecimentos relevantes em entrevistas técnicas**.

---

## Visão Geral do Sistema

O GuitarGPT gira em torno de alguns conceitos centrais:

- **Usuário (User)** – representa o músico que utiliza a plataforma.
- **Projeto Musical (Musical Project)** – um projeto ou ideia de música que agrupa trilhas e interações com IA.
- **Trilha (Track)** – uma trilha ou stem de áudio (por exemplo, guitarra, backing track, baixo, bateria) associada a um projeto.
- **Template de Prompt (Prompt Template)** – templates reutilizáveis usados para montar prompts para modelos de IA (por exemplo, “gerar um solo no estilo de…”).
- **Requisição de Geração (Generation Request)** – um pedido para um agente de IA gerar algum conteúdo (tablaturas, rotinas de estudo, sugestões harmônicas etc.).

Fluxo típico:

1. Um usuário cria um **projeto musical** e adiciona **trilhas** a ele.
2. O usuário seleciona ou personaliza um **template de prompt**.
3. O usuário cria uma **requisição de geração** (por exemplo, “gerar um solo sobre essa progressão”).
4. O backend armazena a requisição e **publica um evento no Kafka**.
5. Um worker/serviço separado (fora deste repositório) pode consumir o evento no Kafka, chamar um provedor de IA e atualizar o status e o resultado da requisição.

Essa separação permite que a API permaneça responsiva enquanto o processamento mais pesado acontece de forma assíncrona.

---

## Visão da Arquitetura

O sistema segue uma **arquitetura hexagonal (ports & adapters)** e um estilo **orientado a eventos**. Em alto nível:

- O **domínio** contém o modelo de negócio central e as ports (interfaces).
- A camada de **aplicação** implementa os casos de uso e orquestra a lógica de domínio.
- A camada de **infraestrutura** contém:
  - adapters web (controllers REST + DTOs),
  - adapters de persistência (JPA + PostgreSQL),
  - adapters de mensageria (produtores Kafka).

Uma visão simplificada das principais interações:

flowchart LR
    User[Usuário / Frontend] --> API[REST API (Spring Web)]
    API --> UC[Camada de Aplicação<br/>Use Cases]
    UC --> DOM[Domínio]
    UC --> DB[(PostgreSQL)]
    UC --> KAFKA[(Kafka)]

    subgraph Infra
        API
        DB
        KAFKA
    end

    KAFKA --> AIWORKER[Future AI Worker<br/>(serviço externo)]
