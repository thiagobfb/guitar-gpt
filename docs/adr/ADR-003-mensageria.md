# ADR 003 — Mensageria: Apache Kafka vs. Fila Gerenciada
**Status:** Aprovado  
**Data:** 2026-02-12  
**Responsável:** Tech Co-Founder (projeto GuitarGPT)

## Contexto
O fluxo de geração com IA é assíncrono: a API recebe a requisição de geração, persiste o pedido e publica um evento para processamento por um worker externo. Precisamos de mensageria que suporte **eventos** e potencial evolução para **streams** (ex.: histórico de gerações, reprocessamento).

## Opções
1. **Apache Kafka** — log distribuído com tópicos particionados, alto throughput, retenção configurável.
2. **Fila gerenciada** (ex.: AWS SQS + SNS, GCP Pub/Sub) — simples, escalável, menor esforço operacional.
3. RabbitMQ — filas e roteamento flexíveis, bom para padrões de fila/worker.

## Decisão
Adotar **Apache Kafka** para publicação e consumo de eventos de geração.

## Justificativa (Trade-offs)
- **Prós (Kafka):**
  - Modelo de **log imutável** e **retenção** que facilita reprocessamento e auditoria.
  - Escala e throughput adequados para cargas de eventos.
  - Valor demonstrável em portfólio para arquitetura orientada a eventos.
- **Contras:**
  - Overhead operacional maior que filas gerenciadas (clusters, armazenamento, tuning).
  - Complexidade adicional para cenários simples.
- **Por que não fila gerenciada agora:**
  - Objetivo de **demonstração de padrões** (streams/event sourcing leve) e independência de cloud específica em ambiente local.

## Consequências
- Uso de **Spring Kafka** para produtores (e futuros consumidores).
- Tópicos versionados por contrato de evento (ex.: `generation.requested.v1`).
- Necessidade de infraestrutura local (Docker Compose) para Kafka/Zookeeper (ou Redpanda) durante desenvolvimento.

## Reversibilidade
Média. Migrar para SQS/SNS ou Pub/Sub exigiria novos adaptadores, mas as portas de mensageria na arquitetura hexagonal reduzem impacto no domínio e casos de uso.
