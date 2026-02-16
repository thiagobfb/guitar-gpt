# ADR 001 — Linguagem/Runtime: Java 21 LTS vs. Java 17 LTS
**Status:** Aprovado  
**Data:** 2026-02-12  
**Responsável:** Tech Co-Founder (projeto GuitarGPT)

## Contexto
O backend utiliza Spring Boot 3.x e arquitetura hexagonal. Precisamos escolher a versão do Java LTS para desenvolvimento e produção. O projeto tem objetivos de portfólio e demonstração de padrões modernos, mas também requer estabilidade e suporte do ecossistema.

## Opções
1. **Java 21 (LTS)** — versão LTS mais recente, novos recursos de linguagem e melhorias de JVM.
2. **Java 17 (LTS)** — amplamente adotado, suporte sólido e grande compatibilidade.
3. Adiar decisão e utilizar versão não-LTS — risco desnecessário para produção.

## Decisão
Adotar **Java 21 (LTS)** como versão padrão de desenvolvimento e runtime.

## Justificativa (Trade-offs)
- **Prós (Java 21):**
  - Suporte de longo prazo e **janela de vida útil** maior que Java 17.
  - Recursos modernos (ex.: `record`, melhorias de desempenho e da JVM) úteis para DTOs/commands.
  - Alinhamento com otimizações recentes do Spring Boot 3.x.
- **Contras:**
  - Alguns ambientes legados e bibliotecas antigas ainda focam em Java 17 — risco baixo para este projeto.

## Consequências
- Pipelines e containers padronizados em Java 21.
- Requer validação de compatibilidade de dependências, especialmente nativas/ALPN.
- Facilita demonstração de recursos modernos em entrevistas técnicas.

## Reversibilidade
Alta. Podemos voltar a Java 17 com ajustes mínimos em build e pipeline, caso alguma dependência crítica não seja compatível.
