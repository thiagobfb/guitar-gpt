# Arquitetura do GuitarGPT — Guia Didático

Este documento explica a arquitetura hexagonal do projeto usando exemplos reais do código-fonte. Se você abrir as packages e se perguntar "por que existem duas classes para User?", este guia é para você.

---

## 1. Visão Geral: Por que duas classes para a mesma coisa?

Ao navegar o projeto, você vai encontrar estas classes para representar um usuário:

| Classe | Package | Tem `@Entity`? | Papel |
|---|---|---|---|
| `User` | `domain.model` | Não | Modelo de domínio puro (POJO) |
| `UserJpaEntity` | `infrastructure.persistence.entity` | Sim | Mapeamento JPA/Hibernate para o banco |

**A separação é intencional.** O domínio não sabe que o banco de dados existe. Quem faz a "ponte" entre os dois mundos é um conjunto de classes na camada de infraestrutura.

---

## 2. As Três Camadas

```
┌─────────────────────────────────────────────────────────────────┐
│                      INFRASTRUCTURE                             │
│                                                                 │
│  ┌──────────────┐    ┌──────────────────┐    ┌───────────────┐  │
│  │  Controller   │    │  JPA Entity      │    │  Kafka        │  │
│  │  + DTOs       │    │  + JPA Repo      │    │  Publisher /  │  │
│  │  (web/)       │    │  + Mapper        │    │  Consumer     │  │
│  │               │    │  + Adapter       │    │  (messaging/) │  │
│  └──────┬───────┘    │  (persistence/)  │    └──────┬───────┘  │
│         │            └────────┬─────────┘           │          │
│─────────┼─────────────────────┼─────────────────────┼──────────│
│         ▼                     ▲                     ▲          │
│    port/in                port/out              port/out       │
│   (interfaces)           (interfaces)          (interfaces)    │
│─────────┬─────────────────────┼─────────────────────┼──────────│
│         │        APPLICATION                        │          │
│         │                                           │          │
│         ▼                                           │          │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  Service (@Service @Transactional)                      │   │
│  │  Implementa port/in, depende de port/out                │   │
│  └─────────────────────────────────────────────────────────┘   │
│─────────────────────────────────────────────────────────────────│
│                        DOMAIN                                   │
│                                                                 │
│  ┌──────────┐  ┌──────────┐  ┌──────────────────────────────┐  │
│  │  Model   │  │ Exception│  │  Port interfaces (in + out)  │  │
│  │  (POJOs) │  │          │  │                              │  │
│  └──────────┘  └──────────┘  └──────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### Domain (centro — zero dependências de framework)

Contém apenas Java puro. Nenhum `import` de Spring, JPA, Kafka ou Jakarta aqui.

- **`domain/model/`** — POJOs com Lombok: `User`, `MusicalProject`, `Track`, etc.
- **`domain/port/in/`** — Interfaces que definem *o que* a aplicação faz (ex: `UserUseCase`)
- **`domain/port/out/`** — Interfaces que definem *o que* a aplicação precisa do mundo externo (ex: `UserRepository`)
- **`domain/exception/`** — Exceções de negócio (`BusinessRuleException`, `ResourceNotFoundException`)

### Application (orquestração)

- **`application/service/`** — Implementa os ports *in* e depende dos ports *out*. Aqui fica a lógica de negócio: validações, orquestração, geração de IDs, controle de status.

### Infrastructure (adaptadores — tudo que depende de framework)

- **`infrastructure/web/`** — Controllers REST + DTOs (request/response)
- **`infrastructure/persistence/`** — JPA entities, Spring Data repos, mappers, adapters
- **`infrastructure/messaging/`** — Kafka publisher e consumer
- **`infrastructure/config/`** — Beans de configuração (OpenAPI, etc.)

---

## 3. Fluxo Completo: Criar um Usuário

Vamos acompanhar uma requisição `POST /api/v1/users` do início ao fim.

### Passo 1 — Controller recebe o request

```java
// infrastructure/web/controller/UserController.java
@PostMapping
public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
    User user = new User();                    // cria modelo de DOMÍNIO
    user.setName(request.name());
    user.setEmail(request.email());
    User created = userUseCase.create(user);   // chama a porta INBOUND
    return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(created));
}
```

O controller converte o DTO (`CreateUserRequest`) para o modelo de domínio (`User`) e chama a interface `UserUseCase`. Ele **não sabe** qual classe implementa essa interface.

### Passo 2 — Service executa a lógica de negócio

```java
// application/service/UserService.java
@Service
@Transactional
public class UserService implements UserUseCase {

    private final UserRepository userRepository;   // porta OUTBOUND (interface)

    public User create(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new BusinessRuleException("Email already in use: " + user.getEmail());
        }
        user.setId(UUID.randomUUID());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);          // chama a porta OUTBOUND
    }
}
```

O service trabalha **somente com objetos de domínio**. Ele depende da interface `UserRepository` (porta outbound), não de JPA. O `@Transactional` do Spring gerencia a transação automaticamente.

### Passo 3 — Adapter traduz domínio para JPA

```java
// infrastructure/persistence/adapter/UserRepositoryAdapter.java
@Repository
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpaRepository;  // Spring Data JPA
    private final UserMapper mapper;

    public User save(User user) {
        return mapper.toDomain(                         // 3. converte de volta para domínio
            jpaRepository.save(                         // 2. persiste no banco via Hibernate
                mapper.toEntity(user)                   // 1. converte domínio → JPA entity
            )
        );
    }
}
```

Este é o **ponto-chave** que pode causar confusão. O adapter:
1. Recebe um `User` (domínio)
2. Converte para `UserJpaEntity` (JPA) via mapper
3. Salva no banco via Spring Data JPA (Hibernate por baixo)
4. Converte o resultado de volta para `User` (domínio)

### Passo 4 — Mapper faz a conversão bidirecional

```java
// infrastructure/persistence/mapper/UserMapper.java
@Component
public class UserMapper {

    public User toDomain(UserJpaEntity entity) {
        return new User(
            entity.getId(), entity.getName(), entity.getEmail(),
            entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }

    public UserJpaEntity toEntity(User domain) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(domain.getId());
        entity.setName(domain.getName());
        entity.setEmail(domain.getEmail());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
```

### Passo 5 — Spring Data JPA faz o SQL

```java
// infrastructure/persistence/repository/UserJpaRepository.java
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, UUID id);
}
```

O Spring Data gera automaticamente as queries SQL a partir dos nomes dos métodos. O Hibernate cuida do mapeamento `UserJpaEntity ↔ tabela users`.

### Resumo visual do fluxo

```
HTTP Request
    │
    ▼
CreateUserRequest (DTO record)
    │  controller converte
    ▼
User (domain POJO)
    │  service processa
    ▼
User (domain POJO)
    │  adapter.mapper.toEntity()
    ▼
UserJpaEntity (@Entity)
    │  JpaRepository.save()  ←── Hibernate gera INSERT
    ▼
UserJpaEntity (com dados do banco)
    │  adapter.mapper.toDomain()
    ▼
User (domain POJO)
    │  controller converte
    ▼
UserResponse (DTO record)
    │
    ▼
HTTP Response (JSON)
```

---

## 4. Por que essa separação existe?

### O problema que resolve

Em projetos que usam `@Entity` diretamente no domínio:

- O domínio **depende** de JPA/Hibernate — trocar para MongoDB ou DynamoDB exige reescrever a lógica de negócio
- Lazy loading e proxies do Hibernate "vazam" para os services e controllers
- `equals/hashCode` gerados por Lombok (`@Data`) quebram com proxies Hibernate
- Annotations de framework poluem os modelos de domínio

### O que a arquitetura hexagonal resolve

- O **domínio** é Java puro: pode ser testado sem banco, sem Spring, sem nada
- Para trocar o banco, basta criar um novo adapter — services não mudam
- Para trocar REST por gRPC, basta criar novos controllers — services não mudam
- Cada camada tem responsabilidade clara e testável de forma isolada

### O trade-off

Mais arquivos por entidade. Para cada conceito (ex: User), existem:

| # | Arquivo | Camada |
|---|---|---|
| 1 | `User.java` | domain/model |
| 2 | `UserUseCase.java` | domain/port/in |
| 3 | `UserRepository.java` | domain/port/out |
| 4 | `UserService.java` | application/service |
| 5 | `UserJpaEntity.java` | infrastructure/persistence/entity |
| 6 | `UserJpaRepository.java` | infrastructure/persistence/repository |
| 7 | `UserMapper.java` | infrastructure/persistence/mapper |
| 8 | `UserRepositoryAdapter.java` | infrastructure/persistence/adapter |
| 9 | `UserController.java` | infrastructure/web/controller |
| 10 | `CreateUserRequest.java` | infrastructure/web/dto/request |
| 11 | `UpdateUserRequest.java` | infrastructure/web/dto/request |
| 12 | `UserResponse.java` | infrastructure/web/dto/response |

São ~12 arquivos por entidade, mas cada um tem uma única responsabilidade.

---

## 5. Fluxo Assíncrono: Kafka

O fluxo de geração de tablatura usa Kafka para processamento assíncrono.

```
Controller                    Service                     Kafka
    │                            │                          │
    │  POST /generation-requests │                          │
    ├───────────────────────────►│                          │
    │                            │  save (PENDING)          │
    │                            │  publish event ─────────►│
    │  201 Created               │                          │
    │◄───────────────────────────┤                          │
    │                            │                          │
    │                            │         Consumer ◄───────┤
    │                            │            │             │
    │                            │  update (PROCESSING)     │
    │                            │◄───────────┤             │
    │                            │  update (COMPLETED)      │
    │                            │◄───────────┤             │
```

### Publisher (adapter de saída)

```java
// infrastructure/messaging/adapter/KafkaGenerationRequestEventPublisher.java
@Component
public class KafkaGenerationRequestEventPublisher implements GenerationRequestEventPublisher {
    // Implementa a porta outbound do domínio
    // Converte GenerationRequest (domínio) → GenerationRequestEvent (record)
    // Publica no tópico "generation-requests"
}
```

### Consumer (adapter de entrada)

```java
// infrastructure/messaging/consumer/GenerationRequestConsumer.java
@Component
public class GenerationRequestConsumer {
    // @KafkaListener no tópico "generation-requests"
    // Usa GenerationRequestUseCase (porta inbound) para atualizar status
    // PENDING → PROCESSING → COMPLETED (ou FAILED)
}
```

O consumer é um **adapter de entrada** — assim como o controller REST. Ambos recebem estímulos externos e chamam as portas inbound do domínio.

---

## 6. Banco de Dados

### Quem gerencia o schema?

O **Flyway** — não o Hibernate. A configuração `hibernate.ddl-auto: validate` faz o Hibernate apenas **validar** que as entities batem com as tabelas existentes, sem modificar o schema.

As migrations ficam em `src/main/resources/db/migration/`:

```
V1__create_users.sql
V2__create_musical_projects.sql
V3__create_tracks.sql
V4__create_prompt_templates.sql
V5__create_generation_requests.sql
V6__seed_prompt_templates.sql
```

### Foreign keys: UUID columns, não @ManyToOne

As JPA entities armazenam FKs como campos `UUID` simples:

```java
// infrastructure/persistence/entity/MusicalProjectJpaEntity.java
@Column(name = "user_id", nullable = false)
private UUID userId;    // ← UUID puro, NÃO @ManyToOne User
```

Isso evita lazy loading acidental, N+1 queries, e mantém as entities independentes entre si. A validação de existência do pai é feita no service:

```java
// application/service/MusicalProjectService.java
public MusicalProject create(UUID userId, MusicalProject project) {
    userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    // ...
}
```

### Enums como String

Enums de domínio (`GenerationRequestStatus`, `TrackType`) são armazenados como `VARCHAR` no banco. A conversão acontece nos mappers:

```java
// entity → domínio
GenerationRequestStatus.valueOf(entity.getStatus())   // "PENDING" → PENDING

// domínio → entity
domain.getStatus().name()                              // PENDING → "PENDING"
```

---

## 7. Testes

### Service tests (unitários)

Testam a lógica de negócio isolada, mockando as portas outbound:

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock UserRepository userRepository;      // mock da porta outbound
    @InjectMocks UserService service;         // service com mocks injetados
}
```

Como o service depende apenas de **interfaces** (portas), é trivial mockar.

### Controller tests (integração web)

Testam o controller com MockMvc, mockando a porta inbound:

```java
@WebMvcTest(UserController.class)
class UserControllerTest {
    @MockitoBean UserUseCase userUseCase;     // mock da porta inbound
    @Autowired MockMvc mockMvc;
}
```

### Por que os testes não precisam de banco?

Porque o service depende de `UserRepository` (interface), não de `UserJpaRepository` (JPA). O Mockito cria uma implementação fake da interface em runtime.

---

## 8. Mapa de Packages

```
com.guitargpt/
├── domain/                          ← Java puro, ZERO imports de framework
│   ├── model/                       ← POJOs (User, MusicalProject, Track, ...)
│   ├── port/
│   │   ├── in/                      ← Interfaces inbound (UserUseCase, ...)
│   │   └── out/                     ← Interfaces outbound (UserRepository, ...)
│   └── exception/                   ← DomainException, BusinessRuleException, ...
│
├── application/
│   └── service/                     ← @Service + @Transactional (UserService, ...)
│
└── infrastructure/                  ← Tudo que depende de framework
    ├── web/
    │   ├── controller/              ← @RestController (UserController, ...)
    │   ├── dto/request/             ← Java records com Jakarta Validation
    │   ├── dto/response/            ← Java records com método from()
    │   └── exception/               ← @RestControllerAdvice (GlobalExceptionHandler)
    ├── persistence/
    │   ├── entity/                  ← @Entity JPA (UserJpaEntity, ...)
    │   ├── repository/              ← JpaRepository interfaces
    │   ├── mapper/                  ← @Component (UserMapper, ...)
    │   └── adapter/                 ← @Repository (UserRepositoryAdapter, ...)
    ├── messaging/
    │   ├── adapter/                 ← Kafka publisher
    │   ├── consumer/                ← Kafka consumer (@KafkaListener)
    │   └── event/                   ← Event records
    └── config/                      ← @Configuration (OpenApiConfig, ...)
```

---

## 9. Regra de Dependência

A regra fundamental: **dependências apontam para dentro** (em direção ao domínio).

```
Infrastructure → Application → Domain
       ✓             ✓           ✗ (não depende de nada externo)
```

- `UserController` importa `UserUseCase` (porta in) e `User` (modelo) — OK
- `UserService` importa `UserRepository` (porta out) e `User` (modelo) — OK
- `User` não importa nada de Spring, JPA, Kafka — OK
- `UserJpaEntity` **nunca** é importado fora de `infrastructure/persistence/` — OK

Se algum dia precisarmos trocar PostgreSQL por MongoDB, criamos novos arquivos em `infrastructure/persistence/` e **nenhuma linha** de `domain/` ou `application/` muda.
