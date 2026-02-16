# ADR 005 — Documentação da API: SpringDoc OpenAPI vs. Alternativas
**Status:** Aprovado
**Data:** 2026-02-13
**Responsável:** Tech Co-Founder (projeto GuitarGPT)

## Contexto
A API REST do GuitarGPT expõe endpoints para gerenciar usuários, projetos musicais, trilhas, templates de prompt e requisições de geração. Precisamos de documentação interativa e atualizada automaticamente para facilitar o desenvolvimento frontend, testes manuais e demonstração do projeto em portfólio.

## Opções
1. **SpringDoc OpenAPI** — geração automática de spec OpenAPI 3.x a partir dos controllers Spring, com Swagger UI integrado.
2. **Springfox** — biblioteca legada para Swagger 2.x/OpenAPI, amplamente usada mas com manutenção descontinuada.
3. Documentação manual (Markdown/Postman) — controle total, porém alta manutenção e risco de desatualização.

## Decisão
Adotar **SpringDoc OpenAPI** (`springdoc-openapi-starter-webmvc-ui`) para documentação automática da API.

## Justificativa (Trade-offs)
- **Prós (SpringDoc):**
  - Geração automática a partir das anotações Spring MVC e Jakarta Validation existentes — zero duplicação.
  - Swagger UI disponível em `/swagger-ui.html` para testes interativos.
  - Suporte nativo a Spring Boot 3.x e Jakarta EE.
  - Configuração mínima via bean `OpenAPI` programático.
- **Contras:**
  - Dependência adicional no classpath de produção (impacto negligível).
  - Spec gerada pode expor detalhes internos se não houver cuidado com DTOs.
- **Por que não Springfox:**
  - Projeto descontinuado, incompatível com Spring Boot 3.x e Jakarta namespace.

## Consequências
- Swagger UI acessível em `http://localhost:8080/swagger-ui.html` com a aplicação rodando.
- Controllers documentados automaticamente — agrupamento por controller sem necessidade de `@Tag`.
- Validações Jakarta (`@NotNull`, `@NotBlank`, `@Size`) refletidas na spec.
- Configuração centralizada em `OpenApiConfig` (título, versão, descrição).

## Reversibilidade
Alta. Remover a dependência e a classe de configuração é suficiente para reverter a decisão.
