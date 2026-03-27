# ADR 007 — Estratégia de Identificadores: UUID vs. ID Numérico
**Status:** Aprovado
**Data:** 2026-03-27
**Responsável:** Tech Co-Founder (projeto GuitarGPT)

## Contexto
O GuitarGPT utiliza `UUID.randomUUID()` gerado na camada de serviço (antes do `save()`) como estratégia de identificadores para todas as entidades (User, MusicalProject, Track, PromptTemplate, GenerationRequest). Durante revisão de arquitetura, foi questionado se IDs numéricos sequenciais (BIGSERIAL/auto-increment) seriam mais adequados, considerando a preferência conservadora por valores numéricos.

Fatores relevantes para a decisão:
- API REST pública com planos de lançamento como SaaS.
- Fluxo assíncrono com Kafka: `GenerationRequestService.create()` salva e publica evento no mesmo fluxo.
- Deploy alvo em AWS ECS com auto-scaling (múltiplas instâncias simultâneas).
- Arquitetura hexagonal com separação estrita entre domínio e infraestrutura.

## Opções

### Opção A — UUID (v4, gerado na aplicação) ✅
Identificador de 128 bits gerado via `UUID.randomUUID()` no serviço, antes da persistência.

### Opção B — ID numérico sequencial (BIGSERIAL)
Identificador inteiro auto-incrementado, gerado pelo PostgreSQL via sequence (`@GeneratedValue(strategy = IDENTITY)`).

### Opção C — UUID v7 (time-ordered)
UUID ordenado por timestamp (RFC 9562), combina benefícios de UUID com inserção sequencial no B-tree. Evolução futura da Opção A.

## Análise Comparativa

| Critério | UUID (v4) | Numérico (BIGSERIAL) |
|---|---|---|
| Tamanho do índice | 16 bytes | 4-8 bytes |
| Performance de INSERT | Random (page splits no B-tree) | Sequencial (append-only) |
| Legibilidade humana | Baixa (`550e8400-e29b-...`) | Alta (`42`) |
| Segurança (enumeração) | Não enumerável | Enumerável (`/users/1`, `/users/2`) |
| Geração distribuída | Sem coordenação entre instâncias | Requer sequence centralizada no DB |
| Eventos Kafka | ID conhecido antes do save | ID conhecido somente após save |
| Acoplamento com DB | Nenhum (gerado na app) | Alto (`@GeneratedValue`, sequences) |
| Debugging | Mais difícil (ID longo) | Mais fácil (ID curto) |

## Decisão
**Manter UUID (Opção A)**, com possibilidade de evolução para UUIDv7 (Opção C) no futuro.

## Justificativa

### 1. Compatibilidade com Kafka e fluxo assíncrono
O `GenerationRequestService.create()` gera o ID, persiste e publica o evento Kafka no mesmo fluxo:

```java
request.setId(UUID.randomUUID());  // ID conhecido aqui
repository.save(request);
eventPublisher.publish(new GenerationRequestEvent(request.getId()));
```

Com ID numérico, seria necessário aguardar o retorno do banco para obter o ID antes de publicar o evento, criando acoplamento temporal entre persistência e mensageria.

### 2. Segurança da API pública
IDs numéricos sequenciais são enumeráveis. Um atacante pode iterar endpoints para descobrir recursos:

```
GET /api/v1/users/1    → 200
GET /api/v1/users/2    → 200
GET /api/v1/users/3    → 200  ← enumeração trivial
```

UUID elimina esse vetor de ataque (OWASP: Broken Object Level Authorization) sem necessidade de camada adicional de ofuscação ou rate limiting agressivo.

### 3. Deploy distribuído (AWS ECS)
Com múltiplas instâncias ECS gerando registros simultaneamente:
- **UUID**: cada instância gera IDs independentemente, sem coordenação.
- **Numérico**: requer sequence centralizada no PostgreSQL (ponto de contenção) ou alocação de ranges (complexidade adicional).

### 4. Coerência com arquitetura hexagonal
O ID é gerado na camada de aplicação (serviço), sem dependência de mecanismos do banco de dados. Isso mantém o domínio puro e a infraestrutura intercambiável — um princípio central da arquitetura hexagonal adotada pelo projeto.

## Mitigação dos Pontos Fracos

| Ponto fraco | Mitigação |
|---|---|
| Índice maior (16 bytes) | Volume da V1 (milhares de registros) torna a diferença irrelevante. Monitorar com `pg_stat_user_indexes`. |
| Page splits no B-tree | Migrar para UUIDv7 (time-ordered) quando volume justificar. PostgreSQL 16 suporta nativamente. |
| Legibilidade baixa | Primeiros 8 caracteres são suficientes para identificação em logs. Logs estruturados (JSON) facilitam busca por ID completo. |

## Evolução Futura (UUIDv7)
Quando o volume de dados justificar otimização de índice, migrar de UUIDv4 para UUIDv7 (RFC 9562):
- Mantém todos os benefícios do UUID (segurança, distribuído, sem acoplamento).
- Elimina page splits: UUIDv7 é time-ordered, inserções são sequenciais no B-tree.
- Requer apenas trocar `UUID.randomUUID()` por gerador UUIDv7 (ex: `java.util.UUID.timeOrderedUUID()` no Java 25+).
- Não requer migração de dados existentes (UUIDv4 e v7 coexistem no mesmo campo).

## Consequências
- **Positivas**: segurança contra enumeração, independência do banco, compatibilidade com Kafka, suporte a multi-instância sem coordenação.
- **Negativas**: índices maiores, inserção não-sequencial (mitigável com UUIDv7), IDs menos legíveis em debugging.
- **Reversão**: possível mas custosa — requer migração de todas as tabelas, FKs, eventos Kafka e clientes de API. Não recomendada após lançamento.
