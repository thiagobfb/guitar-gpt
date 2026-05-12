# ADR 008 — Testes de Arquitetura com ArchUnit
**Status:** Aprovado
**Data:** 2026-04-28
**Responsável:** Tech Co-Founder (projeto GuitarGPT)

## Contexto
O GuitarGPT adota arquitetura hexagonal com três camadas estritas (`domain`, `application`, `infrastructure`) e uma série de convenções obrigatórias:

- Domínio puro (sem Spring, JPA, Kafka).
- Application só conhece `domain`; nunca importa `infrastructure`.
- Controllers dependem de portas (`port/in`) e nunca acessam repositórios diretamente.
- Convenções de nomenclatura: `*Controller` em `web.controller`, `*JpaEntity` em `persistence.entity`, `*RepositoryAdapter` em `persistence.adapter`, `*Service` em `application.service`.

Essas regras são fáceis de violar acidentalmente: basta um `import` automático da IDE injetando uma classe Spring no domínio, ou um controller que "para ganhar tempo" injeta um repositório. Em revisão de código, esse tipo de violação passa silenciosamente — sobretudo em PRs grandes. À medida que o time crescer (ou que múltiplos agentes/IAs editem o código), o custo de erosão arquitetural sobe rápido.

A pergunta: **como tornar as regras arquiteturais executáveis e parte do build, em vez de mantê-las apenas em documentos como CLAUDE.md e nesta pasta de ADRs?**

## Opções

### Opção A — ArchUnit ✅
Biblioteca Java que permite escrever regras de arquitetura como testes JUnit. Lê bytecode compilado (não precisa subir contexto Spring) e reprova o build se uma regra for violada.

### Opção B — Revisão de código manual
Confiar em CODEOWNERS + checklist de PR para garantir que ninguém viole as camadas.

### Opção C — Análise estática genérica (SonarQube / Checkstyle / PMD / Spotbugs)
Ferramentas que rodam regras de qualidade sobre o código-fonte (estilo, complexidade, smells, alguns padrões de dependência).

### Opção D — Java Platform Module System (JPMS / `module-info.java`)
Usar o sistema de módulos do próprio Java para impedir, em tempo de compilação, que pacotes `domain` enxerguem pacotes `infrastructure`.

### Opção E — Spring Modulith
Framework do ecossistema Spring que define módulos lógicos por pacote e verifica regras de visibilidade entre eles, com integração nativa com Spring Boot.

### Opção F — jQAssistant
Ferramenta que importa o projeto para um banco de grafos (Neo4j) e permite escrever regras em Cypher/XML.

## Análise Comparativa

| Critério | ArchUnit (A) | Revisão (B) | SonarQube (C) | JPMS (D) | Spring Modulith (E) | jQAssistant (F) |
|---|---|---|---|---|---|---|
| Regras executáveis no build | ✅ | ❌ | Parcial | ✅ (compilação) | ✅ | ✅ |
| Granularidade arquitetural fina | ✅ Alta | — | Baixa | Média | Alta (pacotes/módulos) | Muito alta |
| Curva de aprendizado | Baixa (DSL fluente em Java) | — | Média | Alta (refactor de pacotes) | Baixa-média | Alta (Cypher/XML) |
| Custo de manutenção | Baixo | Alto (humano) | Baixo | Alto (refactor inicial) | Baixo | Médio |
| Feedback ao desenvolvedor | Falha de teste com mensagem clara | Comentário em PR | Dashboard externo | Erro de compilação | Falha de teste | Relatório do plugin |
| Dependência de infra externa | Nenhuma | Humanos | Servidor Sonar | Nenhuma | Nenhuma | Neo4j embarcado |
| Integração com JUnit existente | ✅ Nativa | — | Não | — | ✅ | Não |
| Acoplamento com Spring | Nenhum | — | Nenhum | Nenhum | Forte | Nenhum |

## Decisão
**Adotar ArchUnit (Opção A)** como mecanismo principal de governança arquitetural automatizada, executando como parte da suíte de testes em `./mvnw clean verify`.

Dependência adotada: `com.tngtech.archunit:archunit-junit5:1.4.0` (escopo `test`).

## Justificativa

### 1. Regras viram testes — e testes quebram o build
Documentação arquitetural apodrece. ADRs e CLAUDE.md descrevem o "deveria"; ArchUnit força o "é". O teste `domainMustNotUseSpring` falha imediatamente se alguém adicionar `@Service` em uma classe de `domain/`, e o build não passa. Isso elimina a categoria inteira de bugs de "esqueci da regra".

### 2. Sem custo de runtime, sem Spring context
ArchUnit lê bytecode compilado via `ClassFileImporter`. Não sobe `ApplicationContext`, não conecta no banco, não inicializa Kafka. A suíte completa de regras roda em centenas de milissegundos — barato o bastante para ser parte do ciclo de feedback local, não só do CI.

```java
@BeforeAll
static void setUp() {
    classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.guitargpt");
}
```

### 3. DSL declarativa e legível
Regras se leem como inglês:

```java
noClasses()
    .that().resideInAPackage("com.guitargpt.domain..")
    .should().dependOnClassesThat().resideInAPackage("org.springframework..")
    .because("Domain must be framework-free")
    .check(classes);
```

A cláusula `.because(...)` é exibida na falha — quem viola a regra entende imediatamente o motivo, sem precisar caçar o ADR.

### 4. Suporte nativo à arquitetura em camadas
A API `layeredArchitecture()` modela hexagonal diretamente:

```java
layeredArchitecture()
    .layer("Domain").definedBy("com.guitargpt.domain..")
    .layer("Application").definedBy("com.guitargpt.application..")
    .layer("Infrastructure").definedBy("com.guitargpt.infrastructure..")
    .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure")
    .whereLayer("Application").mayOnlyBeAccessedByLayers("Infrastructure")
    .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
    .check(classes);
```

Uma regra cobre toda a topologia — não precisamos manter dezenas de testes individuais por par de camadas.

### 5. Convenções de nomenclatura como contrato
O projeto convencionou sufixos (`*Controller`, `*JpaEntity`, `*RepositoryAdapter`, `*Service`). ArchUnit transforma essas convenções em invariantes: se alguém criar um `UserController` fora de `infrastructure.web.controller`, o teste `controllersMustBeInCorrectPackage` quebra. O sufixo deixa de ser sugestão e vira contrato.

## Como ArchUnit se aplica como teste de integração

ArchUnit ocupa um nicho específico na pirâmide de testes — formalmente é um **teste de unidade** (roda em milissegundos, sem dependências externas), mas o **escopo do que ele verifica é arquitetural/integrativo**: como módulos se conectam, não o que cada um faz.

| Tipo de teste | Pergunta que responde | Exemplo no GuitarGPT |
|---|---|---|
| Unitário (Mockito) | "Esta função tem o comportamento esperado?" | `UserServiceTest` mocka repository e valida regras de negócio |
| Slice (`@WebMvcTest`) | "Este controller responde HTTP corretamente?" | `UserControllerTest` valida status codes, JSON, validação Jakarta |
| Integração (Spring + H2) | "Os componentes integrados se comportam juntos?" | `@SpringBootTest` com banco real |
| **Arquitetural (ArchUnit)** | **"As fronteiras estruturais do sistema continuam íntegras?"** | `ArchitectureTest` (181 linhas, 11 regras) |

A suíte de ArchUnit do projeto está organizada em quatro `@Nested` com `@DisplayName`:

1. **Layer dependency rules** — direção das setas entre `domain → application → infrastructure`.
2. **Domain isolation rules** — ausência de Spring, JPA e Kafka no domínio.
3. **Naming conventions** — cada sufixo no pacote certo.
4. **Dependency direction rules** — controllers usam portas, não repositórios.

Por rodar com `./mvnw verify`, qualquer commit que comprometa a arquitetura é detectado **antes** do code review — o ser humano nunca precisa caçar `import org.springframework.*` em arquivos de domínio.

## Por que não as alternativas

### Por que não revisão manual (B)
Não escala. Erros de import são invisíveis em diffs grandes. Em projetos com IA gerando código, a frequência de violações silenciosas sobe — e o revisor humano vira gargalo. Ainda usamos revisão, mas como camada complementar, não única.

### Por que não SonarQube/Checkstyle (C)
Ferramentas excelentes para qualidade geral (complexidade, duplicação, smells), mas regras arquiteturais granulares ("classe X em pacote Y não pode importar pacote Z") são desajeitadas de expressar. Além disso, exigem servidor externo ou execução separada — perdem o ciclo rápido de "fail-fast no build local".

### Por que não JPMS (D)
Funcionaria — `module-info.java` impediria, em tempo de compilação, que `domain` exportasse para `infrastructure`. Mas o custo de adoção é alto: refactor de pacotes para módulos formais, fricção com bibliotecas que ainda não são modulares (incluindo partes do Spring), e granularidade limitada (não cobre regras de naming nem "controllers não acessam repositórios"). Trade desfavorável para o ganho.

### Por que não Spring Modulith (E)
Boa ferramenta dentro do ecossistema Spring, mas acopla a verificação ao próprio framework — cria a contradição de **usar Spring para garantir que o domínio não use Spring**. ArchUnit é framework-agnóstico, alinhado ao princípio da hexagonal.

### Por que não jQAssistant (F)
Mais poderoso (consultas Cypher sobre grafos de dependência), mas overkill para o tamanho atual do projeto. Maior curva de aprendizado, regras menos legíveis, dependência de Neo4j embarcado. Reconsiderar se a complexidade arquitetural crescer.

## Consequências

- **Positivas**:
  - Arquitetura hexagonal mantém-se íntegra automaticamente, sem depender de disciplina manual.
  - Onboarding facilitado: novos devs (ou novos agentes IA) recebem feedback imediato ao violar uma regra, com mensagem explicativa.
  - ADRs e CLAUDE.md ganham braço executável — a documentação para de divergir do código.
  - Refactors de larga escala ficam mais seguros: se uma mudança quebrar a topologia, o build avisa antes do merge.

- **Negativas**:
  - Curva inicial pequena para escrever regras novas (precisa entender o DSL).
  - Regras muito permissivas dão falsa sensação de segurança; regras muito estritas geram fricção. Manter o conjunto auditado.
  - Acréscimo de ~1 dependência de teste e algumas centenas de ms na suíte (irrelevante na prática).

- **Reversão**: trivial. Remover `archunit-junit5` do `pom.xml` e deletar `ArchitectureTest.java`. Nenhum código de produção depende da biblioteca. As regras voltariam a ser verificadas apenas em revisão humana.

## Evolução Futura

- Adicionar regras adicionais conforme novos padrões emergirem (ex: "DTOs devem ser `record`", "JPA entities não podem usar `@Data`").
- Versionar regras junto com ADRs — cada ADR que define um padrão deve, idealmente, vir acompanhado de uma regra ArchUnit que o garanta.
- Reavaliar Spring Modulith ou jQAssistant **somente** se o projeto crescer para múltiplos módulos lógicos com fronteiras de domínio mais ricas (bounded contexts no sentido DDD estrito).
