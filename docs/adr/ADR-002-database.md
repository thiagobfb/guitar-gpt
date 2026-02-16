# ADR 002 — Banco de Dados: PostgreSQL vs. DynamoDB
**Status:** Aprovado  
**Data:** 2026-02-12  
**Responsável:** Tech Co-Founder (projeto GuitarGPT)

## Contexto
Precisamos de armazenamento para entidades como Usuário, Projeto Musical, Trilha, Template de Prompt e Requisição de Geração. Há relacionamento claro entre entidades e necessidade de consultas compostas (por projeto, por usuário, por status). O projeto valoriza portabilidade, desenvolvimento local e padrões do mercado Java/Spring.

## Opções
1. **PostgreSQL** (Relacional) com Spring Data JPA e migrações via Flyway.
2. **DynamoDB** (NoSQL) com modelagem por tabela única e chaves de partição/sort.
3. Banco em memória/arquivo (H2/sqlite) apenas para dev — não atende produção.

## Decisão
Adotar **PostgreSQL** como banco de dados principal.

## Justificativa (Trade-offs)
- **Prós (PostgreSQL):**
  - Modelo relacional adequado às entidades e relacionamentos do domínio.
  - Ecossistema maduro com **Spring Data JPA**, **Flyway**, suporte a transações, índices e constraints.
  - Excelente **experiência local** (Docker) e portabilidade para cloud (RDS/Cloud SQL).
- **Contras:**
  - Menos adequado que um NoSQL para workloads de latência ultra baixa e escalas massivas em leitura com chave-partição bem definida.
- **Por que não DynamoDB agora:**
  - Complexidade de modelagem por acesso e dependência de cloud específica (AWS), reduzindo portabilidade do portfólio.

## Consequências
- Migrações controladas por **Flyway**.
- JPA para persistência, com cuidado para não “vazar” entidades do domínio.
- Possibilidade de **read models** específicos no futuro (ex.: views/materialized views) para consultas de analytics.

## Reversibilidade
Média. Migrar de PostgreSQL para DynamoDB exigiria refatorar modelo e padrões de acesso, porém a arquitetura hexagonal reduz o acoplamento, mantendo portas e adaptadores isolados.
