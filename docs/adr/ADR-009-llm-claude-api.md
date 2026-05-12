# ADR 009 — Integração com LLM: Anthropic Claude API
**Status:** Aprovado
**Data:** 2026-04-28
**Responsável:** Tech Co-Founder (projeto GuitarGPT)

## Contexto
O fluxo principal do GuitarGPT recebe um prompt do usuário, persiste uma `GenerationRequest`, publica evento Kafka, e o `GenerationRequestConsumer` processa de forma assíncrona, gerando uma tablatura. Até esta iteração, a geração era um **mock estático** (`generateMockTablature`) — uma string ASCII fixa. A próxima etapa do produto exige substituir o mock por uma chamada real a um LLM.

Critérios para a escolha do provedor:
- Qualidade na geração de conteúdo musical/textual longo (tablaturas, explicações didáticas).
- SDK oficial em Java compatível com Spring Boot 3.4.2 / Java 21+.
- Custo previsível e controlável.
- Compatibilidade com a arquitetura hexagonal (provedor deve ser intercambiável).
- Preferência por providers com forte capacidade de raciocínio para tarefas com restrições musicais (escala, tempo, afinação, dificuldade).

## Opções

### Opção A — Anthropic Claude (SDK oficial Java) ✅
Usar `com.anthropic:anthropic-java:2.18.0` apontando para `claude-opus-4-7` por padrão.

### Opção B — OpenAI GPT
SDK Java não-oficial (community-maintained) ou cliente HTTP próprio. Modelos GPT-4/GPT-5.

### Opção C — Google Gemini
SDK Java oficial (Vertex AI). Modelos Gemini 2.x.

### Opção D — LLM local (Ollama / llama.cpp)
Modelo open-source rodando localmente (Llama 3, Mistral, etc.) acessado via REST.

## Análise Comparativa

| Critério | Claude (A) | OpenAI (B) | Gemini (C) | Local (D) |
|---|---|---|---|---|
| SDK Java oficial | ✅ `anthropic-java` 2.18.0 | ❌ Community | ✅ Vertex AI SDK | N/A |
| Qualidade em geração longa | Alta | Alta | Alta | Variável |
| Raciocínio (extended thinking) | ✅ Nativo | ✅ (o-series) | Parcial | Limitado |
| Custo (input / output / 1M tokens) | Médio-alto (Opus) / baixo (Haiku) | Médio | Baixo-médio | Apenas infra |
| Latência | Média | Média | Baixa-média | Depende do hardware |
| Vendor lock-in | Médio (mitigado por port) | Médio | Alto (acopla com GCP) | Nenhum |
| Compatibilidade com AWS (deploy alvo) | API HTTPS direta ou Bedrock | API HTTPS direta | Requer interoperação com GCP | Self-hosted no ECS/EKS |
| Maturidade do SDK | Alta (oficial, ativo) | Baixa-média | Alta | N/A |
| Preferência do time | ✅ | — | — | — |

## Decisão
**Adotar Claude API (Opção A)** com:
- SDK: `com.anthropic:anthropic-java:2.18.0`.
- Modelo padrão: `claude-opus-4-7` (configurável via variável `ANTHROPIC_MODEL`).
- Chave de API via variável de ambiente `ANTHROPIC_API_KEY`.
- Acesso isolado por trás da porta de domínio `TablatureGenerator` (hexagonal).

## Justificativa

### 1. SDK oficial Java de primeira classe
A Anthropic mantém um SDK Java oficial, ativo, com versionamento semântico claro. OpenAI ainda depende de bibliotecas de comunidade — risco operacional não-trivial em produção.

### 2. Qualidade em geração longa e estruturada
Tablaturas exigem coerência tonal, respeito a restrições musicais (escala, tempo, dificuldade) e formatação ASCII consistente. Claude Opus 4 entrega resultados consistentemente bons nesse formato em testes manuais.

### 3. Hexagonal protege contra lock-in
A integração entra como **adapter** atrás de uma porta de domínio (`TablatureGenerator`). Trocar de provedor depois é só implementar nova classe de adapter — o domínio e a application não mudam.

```java
// domain/port/out/TablatureGenerator.java
public interface TablatureGenerator {
    String generate(String userPrompt);
}
```

### 4. Custo controlável via configuração
O modelo é configurável por variável de ambiente. Em desenvolvimento ou para tarefas mais simples, basta exportar `ANTHROPIC_MODEL=claude-haiku-4-5` para reduzir o custo em 1-2 ordens de grandeza, sem mudar código.

### 5. Compatibilidade com deploy AWS
Claude está disponível tanto via API HTTPS direta da Anthropic quanto via AWS Bedrock. O SDK Java suporta os dois backends nativamente — facilita migração para Bedrock no futuro se a unificação de billing/IAM com AWS se mostrar vantajosa.

## Estratégia de Prompt (v1)

System prompt curto e estável, definindo:
- Persona: tutor de violão/guitarra que gera tablaturas em ASCII.
- Formato de saída: ASCII tab com 6 cordas (e/B/G/D/A/E), cabeçalho com afinação, tempo (BPM), e legenda.
- Restrições: respeitar escala/tom requisitados, marcar técnicas (h/p/b/~), não inventar afinações não-padrão sem aviso.
- Idioma: responder em português (mesmo do usuário) para explicações.

User message: o `userPrompt` da `GenerationRequest`, sem transformação.

**Decisões de simplicidade explícitas para o v1:**
- Sem `cache_control` — o system prompt inicial fica abaixo do mínimo de 4096 tokens da feature; adicionar quando crescer.
- Sem extended thinking — começa com geração direta para validar latência e custo na prática.
- Sem streaming — `max_tokens=8000` mantém em zona segura para chamada síncrona.
- Sem template-aware (uso de `PromptTemplate`) — fica para próxima iteração junto com lógica de seleção/composição de templates.

## Configuração

Adicionada em `application.yml`:

```yaml
anthropic:
  api-key: ${ANTHROPIC_API_KEY}
  model: ${ANTHROPIC_MODEL:claude-opus-4-7}
  max-tokens: ${ANTHROPIC_MAX_TOKENS:8000}
```

Profile `dev` sobrescreve com placeholder para que a aplicação suba sem chave real:
```yaml
anthropic:
  api-key: ${ANTHROPIC_API_KEY:not-used-in-dev}
```

## Estrutura de Pacotes

Novo pacote `infrastructure.ai`:
- `infrastructure/ai/config/AnthropicConfig.java` — `@Configuration` que provê `AnthropicClient` como `@Bean`.
- `infrastructure/ai/adapter/ClaudeTablatureGenerator.java` — `@Component` que implementa `TablatureGenerator`.

A regra de naming convention do ArchUnit (ADR-008) para `*Generator` ainda não existe, mas o adapter respeita o princípio de manter implementações de portas dentro de `infrastructure`. Layered architecture é satisfeita: `infrastructure.ai` depende de `domain.port.out`.

## Consequências

- **Positivas**:
  - Geração real de tablaturas, encerrando o débito do mock.
  - Custo dinamicamente ajustável (variável `ANTHROPIC_MODEL`).
  - Provedor isolado atrás de porta — troca futura é mecânica.
  - Caminho aberto para Bedrock se billing/IAM AWS justificar.

- **Negativas**:
  - Custo recorrente (token-based) — necessário monitorar via Actuator/CloudWatch.
  - Latência de chamadas síncronas (segundos a dezenas de segundos) — aceitável em fluxo Kafka assíncrono, inaceitável em request HTTP direto (não é o caso atual).
  - Dependência externa: queda da API Anthropic interrompe geração. Mitigação inicial: marcar `GenerationRequestStatus.FAILED` com `errorMessage`; consumer já tem esse fluxo.
  - Falta de extended thinking pode prejudicar pedidos complexos — reavaliar após primeiras gerações reais.

- **Reversão**: trivial. Swap do adapter por `MockTablatureGenerator` (ou outro provedor). Domínio e application permanecem intocados.

## Evolução Futura

| Item | Quando |
|---|---|
| Adicionar `cache_control` no system prompt | Quando system prompt > 4096 tokens (templates ricos, exemplos few-shot) |
| Habilitar extended thinking (`thinking` + `effort`) | Após coletar feedback sobre qualidade — pedidos complexos podem se beneficiar |
| Migrar para streaming | Se gerações começarem a estourar `max_tokens=8000` ou se UX precisar de feedback incremental |
| Template-aware: ler `PromptTemplate` ativo e compor | Quando a tabela `prompt_templates` for usada de fato pelo produto |
| Backend Bedrock | Se deploy AWS se beneficiar de IAM/billing unificado |
| Métricas de custo (input/output tokens por request) | Após primeiro mês de uso real — instrumentar via Micrometer |
| Rate limiting / retry policy customizada | Se 429s aparecerem em produção |
| Regra ArchUnit "implementações de port out vivem em `infrastructure.{x}.adapter`" | Próxima iteração de governança |
