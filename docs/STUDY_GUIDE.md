# Guia de Estudo — GuitarGPT

> Material didático para apresentar o GuitarGPT como projeto pessoal em entrevistas.
> Todos os exemplos são trechos reais do código — caminhos absolutos no formato `arquivo:linha` para você abrir e ler junto.

---

## 1. O que é o GuitarGPT em uma frase

Plataforma backend que recebe pedidos de geração de tablaturas via API REST, persiste o pedido em PostgreSQL, publica um evento em Kafka e processa a geração de forma assíncrona com **Claude API** (Anthropic). É um *event-driven service* construído em arquitetura hexagonal.

**Por que isso é interessante para entrevista?** Junta três coisas que recrutadores procuram: domínio de negócio claro (música/IA), arquitetura limpa testável, e um fluxo assíncrono real (não é só CRUD).

---

## 2. Stack tecnológica e por quê

### 2.1 Linguagem — Java 21 LTS

- **O que é:** versão LTS (Long Term Support) da plataforma Java, suportada até 2031.
- **Por que escolhi:**
  - LTS = estabilidade e suporte longo (importante para portfólio que envelhece bem).
  - Recursos modernos usados no projeto: `record` (DTOs imutáveis), `var`, `Optional`, switch expressions.
  - Compatível com Spring Boot 3 (que exige Java 17+).
- **Onde no código:** `backend/pom.xml:21` declara `<java.version>21</java.version>`.
- **Curiosidade:** o ambiente de desenvolvimento usa Java 25, mas o `pom.xml` fixa em 21 para garantir reprodutibilidade.

### 2.2 Framework Web — Spring Boot 3.4.2

- **O que é:** framework opinativo da família Spring que reduz boilerplate via autoconfiguração e *starters*.
- **Por que escolhi:**
  - Padrão de mercado em backend Java (chance alta de o entrevistador conhecer).
  - Autoconfiguração: declaro um *starter*, ele já provisiona beans, datasource, metrics, etc.
  - Spring Boot 3.x usa Jakarta EE (`jakarta.*`), alinhado com o futuro do ecossistema.
- **Starters usados** (`backend/pom.xml`):
  - `spring-boot-starter-web` — REST controllers, MVC.
  - `spring-boot-starter-data-jpa` — Hibernate + repositórios.
  - `spring-boot-starter-validation` — Bean Validation (Jakarta).
  - `spring-boot-starter-security` + `spring-boot-starter-oauth2-resource-server` — JWT.
  - `spring-boot-starter-actuator` — observabilidade (`/actuator/health`).
  - `spring-kafka` — produtor/consumidor Kafka.

### 2.3 Persistência — PostgreSQL 16 + Flyway

- **PostgreSQL** escolhido por ter relacionamentos claros entre entidades, suporte transacional sólido e ecossistema maduro (RDS, Cloud SQL). Decisão registrada em `docs/adr/ADR-002-database.md`.
- **Flyway** versiona o schema. Cada arquivo `V{n}__{descrição}.sql` em `backend/src/main/resources/db/migration/` é aplicado uma vez e checked-in. O Hibernate roda em modo `validate` (não cria tabelas), garantindo que o schema é controlado **só** pelas migrations.
- **Exemplo real** (`backend/src/main/resources/db/migration/V1__create_users.sql`):
  ```sql
  CREATE TABLE users (
      id         UUID PRIMARY KEY,
      name       VARCHAR(255) NOT NULL,
      email      VARCHAR(255) NOT NULL UNIQUE,
      created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
      updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
  );
  ```
- **Para a entrevista:** "Por que `ddl-auto: validate` e não `update`?" Resposta: `update` faz o Hibernate *adivinhar* mudanças de schema; em produção isso é arriscado (pode deixar tabelas órfãs, mudar tipos de coluna sem aviso). Flyway dá controle determinístico e auditável.

### 2.4 Mensageria — Apache Kafka (Redpanda local)

- **O que é:** log distribuído, particionado, com retenção configurável. Bom para eventos e *streams*.
- **Por que escolhi** (ver `docs/adr/ADR-003-mensageria.md`):
  - Geração via LLM é lenta (segundos a minutos). Não dá para deixar a API travada esperando — preciso responder `202 PENDING` rápido e processar em background.
  - Log imutável + retenção facilita reprocessamento e auditoria (ex: "rodar de novo todas as gerações que falharam ontem").
- **Localmente uso Redpanda** (compatível com a API do Kafka, mais leve que Kafka + Zookeeper).
- **Onde no código:** o publisher implementa a porta `GenerationRequestEventPublisher` (mais detalhes na seção de arquitetura).

### 2.5 Segurança / IAM — AWS Cognito

- **O que é:** serviço gerenciado da AWS que cuida de cadastro, login, recuperação de senha e emissão de JWTs.
- **Por que Cognito e não Keycloak/Auth0/Spring Authorization Server?** Decisão registrada em `docs/adr/ADR-006-autenticacao-iam.md`.
  - Deploy alvo já é AWS — Cognito é gerenciado, zero infra para manter.
  - Free tier de 50k MAUs (mais que suficiente para portfólio).
  - Backend é só **Resource Server**: valida JWTs assinados pelo Cognito, sem ter senha nenhuma.
- **Vendor lock-in?** Mitigado: como o backend só valida JWT padrão OIDC, migrar para Keycloak ou Auth0 é trocar o `issuer-uri` em `application.yml` — não muda código.
- **Onde no código:** `backend/src/main/java/com/guitargpt/infrastructure/config/SecurityConfig.java` usa `oauth2ResourceServer(jwt)` para validar tokens. Veja a seção 11 (Segurança) para o trecho.

### 2.6 LLM / IA — Claude API (integrado)

- **Estado atual:** `ClaudeTablatureGenerator` (em `infrastructure/ai/adapter/`) chama o modelo `claude-opus-4-7` (configurável via `ANTHROPIC_MODEL`) usando o Anthropic Java SDK 2.18.0.
- **Por que Claude?** Raciocínio estruturado e *long context* — útil para gerar tablaturas com contexto musical (escala, andamento, estilo). Decisão detalhada em `docs/adr/ADR-009-llm-claude-api.md`.
- **Padrão arquitetural:** porta de saída `TablatureGenerator` no domínio; o adapter `ClaudeTablatureGenerator` na infra implementa a porta. O domínio não conhece HTTP, JSON nem SDK da Anthropic — mesma separação usada para Kafka e JPA.
- **System prompt:** instrui o modelo a retornar cabeçalho (BPM, tom, afinação) + tablatura ASCII em 6 cordas + legenda de técnicas + explicação didática em português.
  ```
  e|---0---1---0---|
  B|---1---1---1---|
  G|---0---2---0---|
  D|---2---3---2---|
  A|---3---3---3---|
  E|---x---1---x---|
  ```
- **Custo por geração:** ~$0.08 com `claude-opus-4-7` (~464 tokens input + ~986 output no teste E2E). Métricas custom definidas em `docs/adr/ADR-010-observabilidade-grafana-cloud.md`.
- **Config externalizada:** `ANTHROPIC_API_KEY`, `ANTHROPIC_MODEL` e `ANTHROPIC_MAX_TOKENS` via variáveis de ambiente (`.env` local, gitignored).

### 2.7 Cloud — AWS (deploy alvo)

- **Serviços previstos:** ECS (containers), RDS PostgreSQL, MSK ou Redpanda em ECS, Cognito (já decidido).
- **Por quê:** alinhamento com mercado brasileiro (AWS lidera market share), e Cognito já está casado com a stack.

### 2.8 Frontend (planejado)

`frontend/README.md` declara a stack:
- **React + Vite + TypeScript** — padrão de mercado, build rápido.
- **TanStack Query** — gerência cache de servidor sem reinventar a roda.
- **MSW (Mock Service Worker)** — mocka backend no nível de rede para SDD (Spec Driven Development): UI evolui sem depender do backend pronto.
- **shadcn/ui + Tailwind CSS** — componentes copiados (não dependência), customização total + consistência visual.

### 2.9 Build — Maven

- **Por que Maven e não Gradle?** Mais comum em ambientes corporativos brasileiros, sintaxe declarativa, integração com Spring Boot via parent POM.
- **Wrapper checked-in** (`backend/mvnw`) para que qualquer pessoa rode `./mvnw clean verify` sem instalar Maven.

### 2.10 Lombok 1.18.40

- **O que faz:** gera getters/setters/constructors via anotação (`@Getter`, `@Setter`, `@Builder`, `@NoArgsConstructor`).
- **Cuidado documentado** (`CLAUDE.md`): versão fixada em **1.18.40** (a 1.18.36 gerenciada pelo Spring Boot quebra com Java 25). Nunca usar `@Data` em entidade JPA — quebra `equals`/`hashCode` com proxies do Hibernate (causa problemas em `Set<Entity>` e em fetch lazy).

### 2.11 Documentação API — Springdoc OpenAPI

- Gera automaticamente OpenAPI 3 a partir dos controllers. Swagger UI exposto em `/swagger-ui.html`.
- Configuração em `backend/src/main/java/com/guitargpt/infrastructure/config/OpenApiConfig.java`.

### 2.12 Testes — JUnit 5, Mockito, AssertJ, Spring Boot Test, ArchUnit, H2

Cada um tem seu papel — detalhado na seção 12 (Estratégia de testes).

---

## 3. Arquitetura Hexagonal (Ports & Adapters)

### 3.1 Os três anéis

```
┌─────────────────────────────────────────────────────────────────────┐
│                      INFRASTRUCTURE                                 │
│  ┌──────────────┐  ┌──────────────────┐  ┌─────────────────────┐    │
│  │ Controllers  │  │ JPA Entities     │  │ Kafka Publisher     │    │
│  │ + DTOs       │  │ + Mappers        │  │ + Consumer          │    │
│  │ (web/)       │  │ + Adapters       │  │ (messaging/)        │    │
│  └──────┬───────┘  └────────┬─────────┘  └──────┬──────────────┘    │
│─────────┼───────────────────┼───────────────────┼───────────────────│
│         ▼                   ▲                   ▲                   │
│       port/in           port/out             port/out               │
│─────────┬───────────────────┼───────────────────┼───────────────────│
│         │      APPLICATION  │                   │                   │
│         ▼                                                           │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │ Service (@Service @Transactional) — implementa port/in,      │   │
│  │ depende de port/out                                          │   │
│  └──────────────────────────────────────────────────────────────┘   │
│─────────────────────────────────────────────────────────────────────│
│                            DOMAIN                                   │
│  ┌──────────┐  ┌────────────┐  ┌────────────────────────────────┐   │
│  │ Models   │  │ Exceptions │  │ Ports (in = use cases,         │   │
│  │ (POJOs)  │  │            │  │ out = repos/event publishers)  │   │
│  └──────────┘  └────────────┘  └────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘

Regra de dependência: setas só apontam para dentro.
Domain não conhece Application. Application não conhece Infrastructure.
```

### 3.2 Por que duas classes para um User?

| Classe | Pacote | Responsabilidade |
|---|---|---|
| `User` | `domain.model` | POJO puro de negócio. Sem `@Entity`, sem Spring. |
| `UserJpaEntity` | `infrastructure.persistence.entity` | Mapeamento JPA/Hibernate, anotações `@Entity`, `@Table`, `@Column`. |

A "ponte" entre os dois é o `UserMapper` — `infrastructure.persistence.mapper.UserMapper`. Veja o lado `toDomain`:

```java
// backend/src/main/java/com/guitargpt/infrastructure/persistence/mapper/UserMapper.java
public User toDomain(UserJpaEntity entity) {
    return new User(entity.getId(), entity.getName(), entity.getEmail(),
                    entity.getCreatedAt(), entity.getUpdatedAt());
}
```

**Por que essa separação?**
- O domínio não pode depender de JPA — se eu trocasse PostgreSQL por DynamoDB amanhã, `User` continuaria igual.
- Anotações JPA forçam construtores no-args, getters/setters mutáveis, e regras estranhas de `equals`/`hashCode`. Isolar isso na infra mantém o domínio limpo.
- Garantido por teste de arquitetura — veja `ArchitectureTest.domainMustNotUseJpa()`.

### 3.3 Anatomia de uma requisição: `POST /api/v1/users`

Trace de um pedido, arquivo por arquivo:

1. **Cliente** → `UserController.create()` em `infrastructure/web/controller/UserController.java`. Recebe `CreateUserRequest` (DTO com Bean Validation).
2. **Controller** monta um `User` de domínio e chama `userUseCase.create(user)`. Perceba: o controller depende da **interface** `UserUseCase`, não da implementação.
3. **`UserService`** (em `application/service/`) implementa `UserUseCase`:
   ```java
   public User create(User user) {
       if (userRepository.existsByEmail(user.getEmail())) {
           throw new BusinessRuleException("Email already in use: " + user.getEmail());
       }
       user.setId(UUID.randomUUID());
       user.setCreatedAt(LocalDateTime.now());
       user.setUpdatedAt(LocalDateTime.now());
       return userRepository.save(user);
   }
   ```
4. **`UserRepositoryAdapter`** (em `infrastructure/persistence/adapter/`) implementa a porta `UserRepository`. Internamente delega para `UserJpaRepository` (Spring Data) e usa o `UserMapper` para converter ida e volta.
5. **Controller** transforma o `User` retornado em `UserResponse` via factory estática `UserResponse.from(created)` e devolve `201 CREATED`.

**Insight didático:** o `UserService` não sabe que existe Hibernate, Spring Data ou PostgreSQL. Ele só conhece a interface `UserRepository` (porta de saída). Trocar persistência é trocar o adapter, sem tocar no service.

### 3.4 ArchUnit garante a arquitetura

A regra "domain não pode depender de infrastructure" não é só convenção — é teste automatizado em `backend/src/test/java/com/guitargpt/architecture/ArchitectureTest.java`:

```java
@Test
void domainMustNotDependOnInfrastructure() {
    noClasses().that().resideInAPackage("com.guitargpt.domain..")
        .should().dependOnClassesThat().resideInAPackage("com.guitargpt.infrastructure..")
        .check(classes);
}

@Test
void domainMustNotUseSpring() {
    noClasses().that().resideInAPackage("com.guitargpt.domain..")
        .should().dependOnClassesThat().resideInAPackage("org.springframework..")
        .check(classes);
}
```

Se alguém (ou um agente de IA) tentar importar `@Service` no domínio, o build falha. Isso é **arquitetura como código**.

---

## 4. DDD-lite aplicado

O projeto não é DDD "puro sangue" (sem Aggregates explícitos com `apply()`, sem Domain Events de primeira classe), mas adota práticas táticas:

- **Modelos de domínio ricos em conceito, anêmicos em comportamento:** `User`, `MusicalProject`, `Track`, `PromptTemplate`, `GenerationRequest`. As regras de negócio vivem nos services (decisão pragmática para um projeto pequeno; em DDD puro elas estariam nos próprios modelos).
- **Linguagem ubíqua:** classes nomeadas como o domínio fala — `MusicalProject` (não `Project`), `GenerationRequest` (não `LlmJob`).
- **Exceções de domínio** como tipos próprios:
  - `DomainException` (raiz, em `domain/exception/`).
  - `BusinessRuleException` — invariante violada (ex: email duplicado). Mapeada para HTTP 409 Conflict.
  - `ResourceNotFoundException` — recurso inexistente. Mapeada para HTTP 404.
- **Agregado implícito:** `MusicalProject` é raiz; `Track` e `GenerationRequest` só existem dentro de um projeto. O `TrackService` valida a existência do parent antes de criar:
  ```java
  // backend/src/main/java/com/guitargpt/application/service/TrackService.java
  projectRepository.findById(projectId)
      .orElseThrow(() -> new ResourceNotFoundException("MusicalProject", projectId));
  ```

---

## 5. SOLID no código real

### S — Single Responsibility Principle

Cada classe tem **um motivo para mudar**:
- `UserService` — orquestra regras de negócio sobre User.
- `UserMapper` — converte entre `User` e `UserJpaEntity`.
- `UserRepositoryAdapter` — adapta a porta de domínio à API do Spring Data.
- `UserController` — traduz HTTP ↔ casos de uso.
- `GlobalExceptionHandler` — centraliza tradução de exceções para resposta HTTP.

Se eu colocasse "salvar no banco + chamar Kafka + validar e-mail" tudo no controller, mudar qualquer uma dessas três coisas mudaria a mesma classe. Separar minimiza o blast radius das mudanças.

### O — Open/Closed Principle

Quero trocar Kafka por AWS SQS sem mexer em `GenerationRequestService`?
- Service depende da interface `GenerationRequestEventPublisher` (em `domain/port/out/`).
- Hoje a implementação é `KafkaGenerationRequestEventPublisher`.
- Amanhã basta criar `SqsGenerationRequestEventPublisher implements GenerationRequestEventPublisher` — Spring injeta a nova implementação, e o service não muda uma linha.

**Aberto para extensão (novos adapters), fechado para modificação (service inalterado).**

### L — Liskov Substitution Principle

Toda implementação respeita o contrato da interface. Exemplo concreto: `UserService implements UserUseCase`. Se em testes eu uso um mock de `UserUseCase` (`@MockitoBean`), o mock funciona no lugar do service real porque o contrato é cumprido. Veja `UserControllerTest`:

```java
@MockitoBean
private UserUseCase userUseCase;  // mock substitui a implementação real sem o controller perceber
```

### I — Interface Segregation Principle

Portas pequenas, focadas. Em vez de uma `UserApi` gigante com 20 métodos, tenho:
- `UserUseCase` (5 métodos: CRUD básico).
- `UserRepository` (6 métodos: persistência + queries específicas).

E mais: a inbound (`UserUseCase`) e a outbound (`UserRepository`) são **interfaces separadas** porque servem clientes diferentes (controllers vs adapters).

### D — Dependency Inversion Principle

Esta é a alma do hexagonal. O `UserService` não depende de `UserJpaRepository` (detalhe concreto do Spring Data). Depende de `UserRepository` (abstração no domínio):

```java
// backend/src/main/java/com/guitargpt/application/service/UserService.java
public class UserService implements UserUseCase {
    private final UserRepository userRepository;  // interface, não classe!

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
```

Quem implementa `UserRepository`? `UserRepositoryAdapter` na infra. Quem injeta? Spring, via construtor.

**Resultado:** o módulo de alto nível (regras de negócio) define a interface. O módulo de baixo nível (Spring Data + Hibernate) implementa. Inversão da direção natural de dependência.

---

## 6. Clean Code aplicado

### 6.1 Nomes intencionais
- `CreateUserRequest` (DTO de entrada) vs `UserResponse` (DTO de saída) vs `User` (modelo de domínio) vs `UserJpaEntity` (mapeamento JPA). Cada nome diz exatamente o papel.
- `ResourceNotFoundException("User", id)` — o construtor recebe o nome do recurso, gerando mensagens consistentes.

### 6.2 Records para DTOs imutáveis

DTOs são `record` Java — imutáveis por padrão, sem boilerplate:

```java
// backend/src/main/java/com/guitargpt/infrastructure/web/dto/request/CreateUserRequest.java
public record CreateUserRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Email @Size(max = 255) String email
) {}
```

Imutabilidade elimina classes inteiras de bug (alterar DTO em meio ao processamento, race conditions, etc).

### 6.3 Validação declarativa, não imperativa

Bean Validation faz a checagem antes do controller rodar. O controller só usa `@Valid`:

```java
@PostMapping
public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) { ... }
```

Não tenho `if (request.name() == null) throw new BadRequestException(...)`. Falhas de validação viram automaticamente um `MethodArgumentNotValidException` capturado no `GlobalExceptionHandler`.

### 6.4 Tratamento centralizado de erros

`infrastructure/web/handler/GlobalExceptionHandler.java` mapeia exceções para respostas HTTP padronizadas:

```java
@ExceptionHandler(ResourceNotFoundException.class)
public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(404, ex.getMessage()));
}

@ExceptionHandler(BusinessRuleException.class)
public ResponseEntity<ErrorResponse> handleBusinessRule(BusinessRuleException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(409, ex.getMessage()));
}
```

Bonus: trata `InvalidFormatException` para enums dando mensagem amigável ("Accepted: [PENDING, PROCESSING, ...]") em vez de stack trace.

### 6.5 Métodos pequenos, intent revelando

```java
@Override
public void delete(UUID id) {
    findById(id);              // valida existência (lança 404 se não existir)
    userRepository.deleteById(id);
}
```

Reuso de `findById` para validar antes de deletar. Sem comentários — o código fala.

---

## 7. Padrões de Projeto usados

| Padrão | Onde | Por quê |
|---|---|---|
| **Repository** | `UserRepository` (porta) + `UserRepositoryAdapter` (impl) | Esconde detalhes de persistência atrás de interface de coleção. |
| **Adapter** | Adapters em `infrastructure/persistence/adapter/` e `infrastructure/messaging/adapter/` | Adapta API externa (Spring Data, Kafka) à porta do domínio. |
| **Mapper / DTO** | Mappers em `persistence/mapper/`, DTOs em `web/dto/` | Separa modelo de transporte (HTTP/DB) do modelo de domínio. |
| **Factory Method estática** | `UserResponse.from(user)` | Construção semântica e única para conversão domain→response. |
| **Builder** | `User.builder().name(...).build()` (Lombok `@Builder`) | Construção legível em testes, sem ordem fixa de parâmetros. |
| **Use Case (Clean Arch)** | Interfaces em `domain/port/in/` | Cada operação de negócio é uma porta de entrada explícita. |
| **Publisher/Subscriber** | `GenerationRequestEventPublisher` + `GenerationRequestConsumer` (Kafka) | Desacopla produção (rápida) de processamento (lento). |
| **Dependency Injection (constructor)** | Todo service e adapter | Imutabilidade do colaborador, testabilidade, sem `@Autowired` em campo. |
| **Specification implícita** | `existsByEmail`, `existsByEmailAndIdNot` | Encapsula consulta de regra de negócio no repository. |

---

## 8. Fluxo assíncrono / Event-driven

O caso mais interessante para entrevista: **POST /api/v1/projects/{pid}/generations**.

```
Cliente              GenerationRequestService           Kafka topic              Consumer
   |                          |                       generation-requests           |
   |-- POST /generations ---->|                              |                      |
   |                          |-- repo.save(PENDING) ------->|                      |
   |                          |-- publisher.publish() ------>|                      |
   |<-- 201 + status:PENDING -|                              |-- consume(event) --->|
   |                          |                              |                      |-- service.update(PROCESSING)
   |                          |                              |                      |-- claudeAdapter.generate() (~18s)
   |                          |                              |                      |-- service.update(COMPLETED + tab)
```

### Por que assíncrono?

Geração via LLM leva segundos a minutos. Se a API ficar bloqueada esperando, três coisas ruins acontecem: (1) timeout do cliente, (2) thread do servidor presa, (3) impossível dar feedback de progresso.

### O service publica logo após salvar

```java
// backend/src/main/java/com/guitargpt/application/service/GenerationRequestService.java
public GenerationRequest create(UUID projectId, UUID promptTemplateId, GenerationRequest req) {
    // ... validações de parent
    req.setId(UUID.randomUUID());
    req.setStatus(GenerationRequestStatus.PENDING);
    GenerationRequest saved = generationRequestRepository.save(req);
    eventPublisher.publish(saved);   // dispara processamento em background
    return saved;
}
```

**Observação importante:** o `eventPublisher.publish()` está **dentro da transação**. Em produção, isso pode causar o problema clássico do *dual-write* (banco commitado, mas Kafka falhou — ou vice-versa). Solução evolutiva: padrão **Transactional Outbox** (gravar evento em tabela na mesma transação, worker separado publica). Mencionar essa awareness em entrevista pontua.

### O consumer atualiza status em três estados

```java
// backend/src/main/java/com/guitargpt/infrastructure/messaging/consumer/GenerationRequestConsumer.java
@KafkaListener(topics = "generation-requests", groupId = "guitargpt-group")
public void consume(GenerationRequestEvent event) {
    GenerationRequest request = generationRequestUseCase.findById(event.id());
    if (request.getStatus() != GenerationRequestStatus.PENDING) {
        return;  // idempotência: ignora reentrega de evento já processado
    }
    try {
        // 1. PROCESSING
        // 2. tablatureGenerator.generate(event.userPrompt()) — chama Claude API (~18s)
        // 3. COMPLETED + resultText
    } catch (Exception e) {
        // FAILED + errorMessage
    }
}
```

**Idempotência via guard de status:** Kafka garante *at-least-once* (uma mensagem pode ser entregue mais de uma vez). O check `if status != PENDING return` impede reprocessamento de pedidos já completados.

### Por que UUID gerado na aplicação (e não autoincrement)?

Porque o ID precisa estar disponível **antes** do `repo.save()` para ir junto no evento Kafka, e porque UUIDs evitam enumeração de recursos via API pública. Decisão completa em `docs/adr/ADR-007-uuid-vs-id-numerico.md`.

---

## 9. Persistência: JPA + Flyway

### 9.1 Schema versionado pelo Flyway

```
backend/src/main/resources/db/migration/
├── V1__create_users.sql
├── V2__create_musical_projects.sql
├── V3__create_tracks.sql
├── V4__create_prompt_templates.sql
├── V5__create_generation_requests.sql
├── V6__seed_prompt_templates.sql
└── V7__add_updated_at_to_tracks.sql
```

Cada migration é imutável depois de aplicada. Mudanças = nova migration `V8__...`.

### 9.2 Entidade JPA enxuta

```java
// backend/src/main/java/com/guitargpt/infrastructure/persistence/entity/UserJpaEntity.java
@Getter @Setter @NoArgsConstructor   // sem @Data!
@Entity @Table(name = "users")
public class UserJpaEntity {
    @Id private UUID id;
    @Column(nullable = false) private String name;
    @Column(nullable = false, unique = true) private String email;
    // ...
}
```

### 9.3 FK como UUID, não `@ManyToOne`

Decisão pragmática: as entidades JPA armazenam FKs como colunas `UUID` planas, sem relacionamentos `@ManyToOne`. Trade-off:
- **Ganho:** simplicidade, controle total sobre quando/como buscar o parent, zero risco de N+1 acidental, JSON serialization simples.
- **Custo:** preciso buscar manualmente o parent quando preciso (mas o projeto raramente precisa).

Em entrevista, mencionar que se o domínio crescesse (ex: relatórios complexos com joins), eu reconsideraria adicionar `@ManyToOne` em casos específicos.

### 9.4 ON DELETE CASCADE no SQL

FKs de filhos têm `ON DELETE CASCADE` (ver `V5__create_generation_requests.sql`). Quando um `MusicalProject` é deletado, suas `GenerationRequests` somem juntas — regra **no banco**, não em código de aplicação.

---

## 10. Configuração por profiles

`backend/src/main/resources/application.yml` define perfis:

| Profile | Datasource | Auth | Kafka |
|---|---|---|---|
| (default) | localhost PostgreSQL | Cognito JWT real | localhost:9092 |
| `dev` | localhost PostgreSQL | **desligada** (`permitAll`) | localhost:9092 |
| `docker` | host `db` (Compose) | Cognito JWT | host `redpanda` |

O `SecurityConfig` usa `@Profile` para carregar a config correta:

```java
// backend/src/main/java/com/guitargpt/infrastructure/config/SecurityConfig.java
@Configuration @EnableWebSecurity @Profile("!dev")
static class JwtSecurityConfig {
    @Bean SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .build();
    }
}

@Configuration @EnableWebSecurity @Profile("dev")
static class DevSecurityConfig { /* anyRequest().permitAll() */ }
```

**Por que stateless?** API REST não tem sessão de usuário no servidor — o JWT carrega tudo. Permite escalar horizontalmente sem *sticky sessions*.

**Por que CSRF disabled?** Não há cookies de sessão; CSRF só protege contra ataques que abusam de cookies persistidos no browser.

### Externalização de credenciais

Sem hardcoding: `application.yml` lê do ambiente:
```yaml
url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:guitargpt}
username: ${DB_USERNAME:guitargpt}
issuer-uri: ${COGNITO_ISSUER_URI}
```
`${VAR:default}` = lê `VAR` do ambiente, cai no default se ausente. `COGNITO_ISSUER_URI` **não tem default** — em produção, faltar essa variável faz o app falhar no boot (fail-fast intencional).

---

## 11. Observabilidade e qualidade

- **Spring Boot Actuator** expõe `/actuator/health` e `/actuator/info`. Health checks usados por load balancer (ALB) para decidir routing.
- **Logs estruturados** via SLF4J + Logback (já incluído no `spring-boot-starter`). Exemplo:
  ```java
  log.info("Received generation request event: id={}, status={}", event.id(), event.status());
  ```
  Parâmetros separados (`{}`) permitem agregação posterior em ferramentas como CloudWatch Insights ou Datadog.
- **OpenAPI / Swagger UI** documenta a API automaticamente.

---

## 12. Estratégia de testes

### 12.1 Pirâmide aplicada

```
       ▲
      / \   ArchUnit (1 classe, ~10 testes)        — guarda da arquitetura
     /---\
    /     \  Controller tests (@WebMvcTest)        — HTTP + serialização + validação
   /-------\
  /         \  Service tests (Mockito puro)         — regras de negócio (maioria)
 /-----------\
```

### 12.2 Service test — Mockito puro, rápido, sem Spring

```java
// backend/src/test/java/com/guitargpt/application/service/UserServiceTest.java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock private UserRepository userRepository;
    @InjectMocks private UserService userService;

    @Test
    void create_shouldThrowWhenEmailExists() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);
        assertThatThrownBy(() -> userService.create(user))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Email already in use");
    }
}
```

- `@ExtendWith(MockitoExtension.class)` ativa Mockito sem subir contexto Spring → roda em milissegundos.
- `@Mock` cria a porta como dublê; `@InjectMocks` injeta no service.
- **AssertJ** dá assertions fluentes (`assertThatThrownBy(...)`).
- Cada teste segue Arrange-Act-Assert (AAA).

### 12.3 Controller test — `@WebMvcTest` com slice mínimo

```java
// backend/src/test/java/com/guitargpt/infrastructure/web/controller/UserControllerTest.java
@Import(TestSecurityConfig.class)
@WebMvcTest(UserController.class)
class UserControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean UserUseCase userUseCase;

    @Test
    void create_shouldReturn400WhenInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"email\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }
}
```

- `@WebMvcTest(UserController.class)` sobe **só** o controller + MVC + Jackson + validação. Não sobe banco, Kafka, security real.
- `@MockitoBean` é a substituição moderna do `@MockBean` (deprecated). Ele troca o bean real do contexto pelo mock.
- `TestSecurityConfig` desliga security em testes (sem isso, todo teste de controller pega 401).
- Verifica **HTTP status, Content-Type e payload JSON** com `jsonPath`.

### 12.4 Consumer test — fluxo de mensageria

`GenerationRequestConsumerTest` mostra três cenários:
- **Caminho feliz:** PENDING → PROCESSING → COMPLETED (verificado com `ArgumentCaptor` para inspecionar cada update).
- **Idempotência:** evento com status já COMPLETED é ignorado (`verify(..., never())`).
- **Falha:** exceção no meio leva a status FAILED com `errorMessage` setado.

### 12.5 ArchUnit — arquitetura testável

Já mostrado na seção 3.4. Reforçando: testes em `ArchitectureTest` falham o build se alguém quebrar:
- Direção das dependências entre camadas.
- Isolamento do domínio (sem Spring/JPA/Kafka).
- Convenções de nome (`*Controller` em `web.controller`, `*JpaEntity` em `persistence.entity`, etc).

### 12.6 H2 em memória

Configuração de teste usa H2 (`com.h2database:h2` em `<scope>test</scope>` no `pom.xml`). Não preciso de Docker para rodar `./mvnw test`.

---

## 13. Resumo das decisões importantes (ADRs)

| ADR | Tema | Decisão | Razão central |
|---|---|---|---|
| 001 | Linguagem | Java 21 LTS | Estabilidade longa, recursos modernos, alinhamento com Spring Boot 3 |
| 002 | Banco | PostgreSQL | Relacionamentos claros, ecossistema Spring Data + Flyway, portabilidade |
| 003 | Mensageria | Apache Kafka | Log imutável, retenção, valor demonstrável de event-driven |
| 004 | Geração de código | Lombok 1.18.40 | Reduz boilerplate; versão fixada por compat Java 25; nunca `@Data` em entidade |
| 005 | Docs API | Springdoc OpenAPI | Documentação automática a partir do código |
| 006 | IAM | AWS Cognito | Gerenciado, free tier, alinhado com deploy AWS, JWT padrão = baixa lock-in |
| 007 | IDs | UUID gerado na aplicação | Compatível com Kafka (ID antes do save), evita enumeração, deploy distribuído sem coordenação |
| 008 | Testes de arquitetura | ArchUnit 1.4.0 | Garante isolamento de camadas no CI — arquitetura como código |
| 009 | LLM | Anthropic Claude API | Raciocínio estruturado, long context, SDK Java oficial; `claude-opus-4-7` default |
| 010 | Observabilidade | Grafana Cloud + Micrometer | Free tier, zero manutenção, métricas custom de custo Claude |
| 011 | Áudio da tablatura | alphaTab in-browser (proposto) | Zero custo por play, MPL-2.0, synth MIDI + render visual numa lib |

---

## 14. O que vem a seguir

- **Frontend** (React + Vite + TS): renderizar a tab em fonte monoespaçada + `alphaTab` para play MIDI in-browser (ADR-011 proposto).
- **Observabilidade**: instrumentar `guitargpt.claude.request.duration`, `guitargpt.claude.tokens`, `guitargpt.claude.cost.usd` no `ClaudeTablatureGenerator` e enviar para Grafana Cloud (ADR-010 aprovado, implementação pendente).
- **Transactional Outbox** para corrigir o dual-write banco↔Kafka.
- **Refinar system prompt** para emitir AlphaTex (formato estruturado do alphaTab) em vez de ASCII livre.
- Ver `docs/BACKLOG.md` para lista completa priorizada.

---

## 15. Perguntas que provavelmente caem em entrevista

**Arquitetura:**
- "Por que arquitetura hexagonal e não MVC tradicional?" → Testabilidade (mocks nas portas), independência de framework, regras de negócio sobrevivem a troca de stack.
- "Qual o overhead da hexagonal? Vale para projeto pequeno?" → Sim, custa mais classes, mas paga em testabilidade. Para CRUD simples sem regras, talvez não valha; o GuitarGPT tem fluxo assíncrono e LLM, justifica.
- "Como você garante que a regra arquitetural não é violada?" → ArchUnit no CI.

**Persistência:**
- "Por que não `@ManyToOne`?" → Simplicidade, evita N+1, mas reconheço o trade-off para queries complexas.
- "Por que UUID e não autoincrement?" → ADR-007: precisava do ID antes do save (Kafka), evita enumeração via API, deploy distribuído.
- "O que acontece se duas instâncias gerarem o mesmo UUID?" → Probabilidade da ordem de 2^-122; na prática, impossível.

**Mensageria:**
- "Como você lida com mensagens duplicadas no Kafka?" → Idempotência por estado (`if status != PENDING return`).
- "E se o Kafka cair entre `repo.save` e `publisher.publish`?" → Hoje é dual-write (problema). Solução: Transactional Outbox.
- "Por que Kafka e não SQS?" → Log imutável, retenção, demonstra conhecimento de event streaming. Em produção AWS-only, SQS é mais simples.

**Segurança:**
- "Por que stateless e por que CSRF off?" → API REST sem sessão; CSRF só faz sentido com cookies.
- "Como funciona a validação do JWT?" → Spring busca JWKS em `issuer-uri/.well-known/jwks.json`, valida assinatura, expira automaticamente.
- "Como autorizar por usuário?" → Próximo passo: extrair `sub` do JWT (Cognito user ID) e usar como filtro nas queries de service.

**Testes:**
- "Por que mocka `UserRepository` em vez de subir banco H2?" → Velocidade. Service test não precisa testar SQL — testa regra de negócio. Banco real fica para teste de integração.
- "Por que `@WebMvcTest` em vez de `@SpringBootTest`?" → Slice menor, sobe só MVC. `@SpringBootTest` sobe app inteiro, lento.

**Clean Code:**
- "Onde estão os comentários no código?" → Praticamente não há. Nomes e tipos comunicam intenção. Comentários só para *por quê* não-óbvio (workarounds, decisões surpreendentes).

---

## Apêndice A — Mapa rápido de pacotes

```
backend/src/main/java/com/guitargpt/
├── domain/
│   ├── model/           # POJOs: User, MusicalProject, Track, PromptTemplate, GenerationRequest
│   ├── exception/       # DomainException, BusinessRuleException, ResourceNotFoundException
│   └── port/
│       ├── in/          # Casos de uso: UserUseCase, TrackUseCase, ...
│       └── out/         # Repositórios e publishers: UserRepository, GenerationRequestEventPublisher, ...
├── application/
│   └── service/         # Implementações dos casos de uso (regras de negócio)
└── infrastructure/
    ├── config/          # SecurityConfig, OpenApiConfig
    ├── web/
    │   ├── controller/  # REST controllers
    │   ├── dto/         # CreateXRequest, XResponse, ErrorResponse
    │   └── handler/     # GlobalExceptionHandler
    ├── persistence/
    │   ├── entity/      # *JpaEntity (mapeamento Hibernate)
    │   ├── mapper/      # Conversão domain ↔ entity
    │   ├── adapter/     # Implementação das portas de saída de persistência
    │   └── repository/  # Spring Data JPA interfaces
    ├── ai/
    │   ├── adapter/     # ClaudeTablatureGenerator (Anthropic Java SDK)
    │   └── config/      # AnthropicConfig (@Bean AnthropicClient)
    └── messaging/
        ├── adapter/     # KafkaGenerationRequestEventPublisher
        ├── consumer/    # GenerationRequestConsumer (@KafkaListener)
        └── event/       # GenerationRequestEvent (record)
```

---

## Apêndice B — Como rodar e demonstrar

```bash
# Subir tudo (Postgres + Redpanda + app)
docker-compose up -d

# Ou só os tests (rápido, usa H2 em memória)
cd backend && mvn test

# Build completo
cd backend && mvn clean verify

# Acessar Swagger UI
open http://localhost:8080/swagger-ui.html
```

> **WSL**: `./mvnw` falha por CRLF nos line endings. Use o Maven do sistema (`mvn`) ou adicione o caminho explícito, ex: `/mnt/c/dev/apache-maven-3.9.12/bin/mvn`.

Para demo em entrevista:
1. Mostrar `ArchitectureTest.java` rodando — "minha arquitetura é testável".
2. Mostrar `UserController` → `UserService` → `UserRepositoryAdapter` — "regra inverte para o domínio".
3. POST em `/api/v1/projects/{id}/generations` → mostrar status `PENDING` na resposta → aguardar ~18s → consultar `/generations/{id}` mostrando `COMPLETED` com tablatura gerada pelo Claude — "fluxo assíncrono real: Kafka + LLM".
