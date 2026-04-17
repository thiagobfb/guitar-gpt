# GuitarGPT – Backend para uma Plataforma de Prática de Guitarra com IA

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

**Fluxo típico**:

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

```mermaid
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
```
---

## Contexto adicional do projeto

Você mencionou que este sistema é voltado para guitarristas que também são desenvolvedores de software. O objetivo inicial é praticar stacks comuns no mercado e pedidas em vagas para **dev Java**, aplicando conhecimentos da pós em **engenharia de software com IA** (prompt engineering, LangChain, adição de contextos e uso de MCPs). Este documento consolida essa visão e pode servir como base para ADRs, NFRs e planejamento da V1.
