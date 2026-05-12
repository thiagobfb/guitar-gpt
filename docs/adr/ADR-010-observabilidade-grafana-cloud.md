# ADR 010 — Observabilidade: Grafana Cloud (free tier) + Micrometer
**Status:** Aprovado
**Data:** 2026-04-28
**Responsável:** Tech Co-Founder (projeto GuitarGPT)

## Contexto
Após a integração com a Claude API (ADR-009), o GuitarGPT passou a operar com três componentes assíncronos cujo comportamento em produção precisa ser visível:

1. **API REST** (Spring Boot) — taxa de requests, latência por endpoint, erros 5xx.
2. **Consumer Kafka** (`GenerationRequestConsumer`) — lag de consumer, throughput, taxa de falha.
3. **Adapter Claude** (`ClaudeTablatureGenerator`) — latência da chamada externa, contagem de tokens (input/output), taxa de erro, **custo em USD**.

Hoje a observabilidade do projeto resume-se a:
- `/actuator/health` e `/actuator/info` (apenas dois endpoints expostos).
- Logs via SLF4J/Logback em formato texto.
- Nenhuma métrica customizada, nenhum dashboard, nenhum tracing, nenhum alerta.

Pontos cegos críticos:
- **Custo Claude desconhecido** — `ClaudeTablatureGenerator` loga tokens, mas não agrega. Sem isso, não há projeção mensal nem alarme de billing.
- **Latência fim-a-fim** — quanto tempo leva entre `POST /generation-requests` e `status=COMPLETED`? Hoje só dá pra saber lendo timestamps no banco.
- **Lag Kafka** — se a fila crescer, ninguém percebe até usuário reclamar.
- **Erros silenciosos** — exceptions caem no `catch` do consumer, marcam `FAILED`, mas não disparam nada externo.

Critérios para a escolha (do usuário): **gratuito ou de baixo custo**, **fácil de configurar**, **UI amigável**.

## Opções

### Opção A — Grafana Cloud (free tier) + Micrometer + Loki ✅
SaaS gerenciado com plano gratuito. Ingest de métricas Prometheus + logs Loki + traces Tempo. Dashboards Grafana já hospedados.

### Opção B — Stack self-hosted: Prometheus + Grafana + Loki em Docker
Mesma stack, rodando em containers locais (e depois no AWS ECS).

### Opção C — AWS CloudWatch
Serviço nativo da AWS (deploy alvo do projeto). Métricas, logs e dashboards integrados ao IAM.

### Opção D — New Relic (free tier)
APM SaaS. Free tier de 100GB/mês, 1 usuário full-platform.

### Opção E — Datadog (free tier)
APM SaaS. Free tier muito limitado: 5 hosts, 1 dia de retenção.

### Opção F — OpenTelemetry + Jaeger (self-hosted, foco em tracing)
Tracing distribuído puro, sem métricas/logs nativos.

### Opção G — Spring Boot Admin
Painel dedicado a apps Spring, leitor de Actuator.

## Análise Comparativa

| Critério | Grafana Cloud (A) | Self-hosted GLP (B) | CloudWatch (C) | New Relic (D) | Datadog (E) | OTel+Jaeger (F) | SBA (G) |
|---|---|---|---|---|---|---|---|
| Custo (uso esperado V1) | **Grátis** (até 10k métricas/50GB logs) | Grátis (paga infra) | Pago (~$5-20/mês em escala pequena) | Grátis (até 100GB) | Limitado (5 hosts) | Grátis | Grátis |
| Setup inicial | ✅ Baixo (1 agente) | Médio (3 containers + scrape config) | Médio (instrumentar SDK AWS) | ✅ Muito baixo (1 agente) | Baixo | Alto (manual) | ✅ Trivial |
| UI (dashboards) | ✅ Grafana (referência de mercado) | ✅ Grafana | Médio | ✅ Excelente | ✅ Excelente | Fraco | OK (focado em Spring) |
| Métricas custom | ✅ Micrometer/Prometheus | ✅ | ✅ (via SDK) | ✅ | ✅ | Limitado | Limitado |
| Logs estruturados | ✅ Loki | ✅ Loki | ✅ CloudWatch Logs | ✅ | ✅ | Não | Não |
| Tracing | ✅ Tempo | ✅ Tempo/Jaeger | ✅ X-Ray | ✅ | ✅ | ✅ | Não |
| Alertas | ✅ Grafana Alerting | ✅ | ✅ CloudWatch Alarms | ✅ | ✅ | Não | Limitado |
| Custo de manutenção | **Zero** | Médio (3 containers para subir e manter) | Baixo | Zero | Zero | Alto | Zero |
| Vendor lock-in | Baixo (formatos abertos) | Nenhum | Alto (AWS) | Médio | Alto | Nenhum | Baixo |
| Migração futura | Fácil (export para self-hosted) | N/A | Difícil | Médio | Médio | N/A | N/A |

## Decisão
**Adotar Grafana Cloud free tier (Opção A)** como stack primária de observabilidade, com **Spring Boot Admin (Opção G) como complemento para dev local**.

Componentes:
- **Métricas**: `micrometer-registry-prometheus` no app → `/actuator/prometheus` → scrapeado por **Grafana Alloy** (agente leve) → enviado para Grafana Cloud Mimir (Prometheus-compatível).
- **Logs**: Logback em formato JSON → Grafana Alloy lê arquivos / stdout → Grafana Cloud Loki.
- **Tracing**: opcional no V1; ativar OpenTelemetry SDK quando necessário → Grafana Cloud Tempo.
- **Dashboards**: importar templates prontos ("Spring Boot 3 Statistics", "Kafka Consumer", "JVM Micrometer") + 1 dashboard custom para métricas Claude.
- **Alertas**: configurados via Grafana Alerting (no próprio Grafana Cloud).
- **Dev local**: Spring Boot Admin em container separado para inspeção rápida sem precisar do Grafana Cloud.

## Justificativa

### 1. Custo zero no horizonte previsível
Free tier do Grafana Cloud (em 2026): **10.000 séries de métricas, 50GB de logs, 50GB de traces, 14 dias de retenção, 3 usuários**. O GuitarGPT em V1 vai gerar ~200-500 séries (Spring Boot Actuator default + handful de métricas custom) e algumas centenas de MB de logs/dia. **Sobra muita margem antes de pagar qualquer coisa.**

### 2. Baixo custo de setup e manutenção
Setup completo:
- Criar conta Grafana Cloud (free).
- Adicionar 1 dependência (`micrometer-registry-prometheus`).
- Habilitar `prometheus` em `management.endpoints.web.exposure.include`.
- Instalar e configurar **Grafana Alloy** (agente único que substitui Promtail, Grafana Agent, OTel Collector). Pode rodar como sidecar no ECS ou container `docker-compose` em dev.
- Importar dashboards via UI (3 cliques cada).

**Manutenção**: zero. Atualizações de stack ficam por conta do Grafana Labs.

### 3. UI de referência da indústria
Grafana é o padrão de fato em observabilidade open-source. Em entrevista, mostrar dashboards Grafana com métricas custom de LLM é universalmente reconhecido. New Relic e Datadog têm UIs ótimas também, mas com vendor lock-in maior.

### 4. Casa com o stack atual sem fricção
- `spring-boot-starter-actuator` já está no projeto.
- Micrometer é o padrão Spring Boot (vem com Actuator).
- Adicionar `micrometer-registry-prometheus` ativa o endpoint sem mais config.
- Logback → JSON exige só uma config em `logback-spring.xml`.
- Hexagonal preservada: instrumentação fica em `infrastructure.observability/` ou inline no adapter — domínio não conhece Micrometer.

### 5. Caminho de saída claro se Grafana Cloud não servir
Self-hosted (Opção B) usa **a mesma stack** (Prometheus, Grafana, Loki). Trocar é mover o `remote_write` do Alloy para um endpoint próprio. Zero refactor de código.

### 6. Por que NÃO CloudWatch como primeira escolha
Apesar do deploy alvo ser AWS, CloudWatch:
- UI inferior a Grafana para dashboards de séries temporais.
- Custo cresce rápido com volume de logs (paga ingest + retenção).
- Vendor lock-in alto (queries em Logs Insights são proprietárias).
- Pode coexistir depois (CloudWatch para logs de infra ECS + Grafana Cloud para métricas/logs da app).

## Métricas customizadas a instrumentar (V1)

A regra: **instrumentar onde dói**. Para o fluxo Claude:

| Métrica | Tipo | Tags | Por quê |
|---|---|---|---|
| `guitargpt.claude.request.duration` | Timer | `model`, `outcome` (success/error) | Latência por modelo, identificar regressões |
| `guitargpt.claude.tokens` | Counter | `model`, `direction` (input/output) | Volume agregado para projeção de custo |
| `guitargpt.claude.cost.usd` | Counter | `model` | Custo acumulado em USD (calculado pelo adapter a partir de tokens × tabela de preços) |
| `guitargpt.generation.queue.lag` | Gauge | (sem tags) | Backlog do consumer Kafka — alerta se passar de N |

Métricas que vêm de graça pelo Spring/Micrometer (não precisa instrumentar):
- `http.server.requests` — taxa, latência, status code por endpoint.
- `kafka.consumer.fetch.manager.records.lag.max` — lag por partição (via spring-kafka).
- `jvm.memory.*`, `jvm.gc.*`, `process.cpu.*` — health da JVM.
- `hikaricp.connections.*` — pool do banco.

## Estratégia de dashboards (V1)

1. **Health geral** (importado: "Spring Boot 3 Statistics") — JVM, HTTP, datasource.
2. **Kafka Consumer** (importado: "Kafka Consumer Lag") — lag, throughput, rebalances.
3. **GuitarGPT Generation** (custom) — fila pendente, latência fim-a-fim, taxa de sucesso/falha.
4. **Claude API** (custom) — chamadas/min, p50/p95/p99 latência, **tokens/h**, **$/h acumulado**, taxa de erro.

## Estratégia de alertas (V1)

Mínimo viável (todos via Grafana Alerting → Slack ou e-mail):
- `5xx rate > 1% por 5min` — bug ou Claude fora do ar.
- `kafka consumer lag > 100 por 10min` — fila acumulando.
- `claude $/h projetado > $X` — gasto inesperado.
- `health check down por 1min` — app caída.

## Consequências

- **Positivas**:
  - Visibilidade real do custo Claude por hora/dia, sem depender de planilha manual.
  - Dashboards prontos em < 1 dia de trabalho.
  - Alertas básicos cobrem incidentes mais comuns.
  - UI em entrevista demonstra maturidade operacional.
  - Sem custo financeiro no horizonte de V1.

- **Negativas**:
  - Mais um SaaS para gerenciar (conta, créditos, retenção).
  - Limites do free tier podem ser atingidos se logs explodirem (precisa monitorar o próprio consumo no Grafana Cloud usage page).
  - Ingest de logs depende de conectividade — se Grafana Cloud cair, perde-se logs do período (mitigável com buffer local do Alloy).
  - Adicionar instrumentação custom no `ClaudeTablatureGenerator` injeta `MeterRegistry` no adapter — aceitável (faz parte da infraestrutura), mas é mais um collaborator no construtor.

- **Reversão**:
  - Trocar para Opção B (self-hosted) é mudança de config no Alloy, sem tocar código.
  - Trocar para CloudWatch exige reinstrumentar — rota de saída mais cara.
  - Remover toda observabilidade: dropar dependência `micrometer-registry-prometheus` e Alloy.

## Evolução Futura

| Item | Quando |
|---|---|
| Habilitar OpenTelemetry tracing | Quando começar a debugar latência fim-a-fim entre publish Kafka → Claude → save |
| Logs JSON estruturados (`logback-spring.xml` + `logstash-logback-encoder`) | Junto com a primeira instrumentação Loki |
| Migrar para Grafana self-hosted | Se passar do free tier OU se compliance/dados sensíveis exigirem |
| Adicionar CloudWatch para logs de infra ECS | No primeiro deploy AWS — coexistência com Grafana Cloud |
| RED method para todos os endpoints | Quando houver mais clientes (web, mobile) |
| SLOs formais (latência p95 < Xs, sucesso > Y%) | Quando o produto tiver usuários pagantes |
| Regra ArchUnit "adapters podem importar Micrometer; domínio e application não" | Junto com a primeira instrumentação |
