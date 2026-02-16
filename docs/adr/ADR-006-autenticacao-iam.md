# ADR 006 — Autenticação/IAM: AWS Cognito vs. Keycloak
**Status:** Aprovado
**Data:** 2026-02-13
**Responsável:** Tech Co-Founder (projeto GuitarGPT)

## Contexto
O backend precisa de autenticação e autorização para proteger os endpoints REST. A plataforma será implantada na AWS, o que influencia a escolha da solução de IAM. O projeto usa Spring Boot 3.x com arquitetura hexagonal, e a solução de auth deve ficar inteiramente na camada de infraestrutura, sem contaminar o domínio.

Requisitos:
- OAuth2/OIDC com JWT para stateless authentication.
- Gerenciamento de usuários (registro, login, recuperação de senha).
- Integração com Spring Security (`spring-boot-starter-oauth2-resource-server`).
- Mínima complexidade operacional em produção.

## Opções
1. **AWS Cognito** — Serviço gerenciado de autenticação da AWS com User Pools e Identity Pools.
2. **Keycloak (self-hosted na AWS)** — IAM open-source rodando em ECS/Fargate.
3. **Auth0 (SaaS)** — Plataforma de identidade gerenciada por terceiro.
4. **Spring Authorization Server** — Servidor OAuth2 nativo do ecossistema Spring.

## Decisão
Adotar **AWS Cognito** como provedor de identidade e autenticação.

## Justificativa (Trade-offs)

### AWS Cognito (escolhido)
- **Prós:**
  - Serviço gerenciado — zero infraestrutura adicional para manter.
  - Integração nativa com serviços AWS (ALB, API Gateway, Lambda, IAM roles).
  - Free tier generoso (50.000 MAUs).
  - Mesma integração Spring Security via `oauth2-resource-server` + JWT (`issuer-uri` apontando para o Cognito User Pool).
  - Hosted UI para login/registro (opcional, acelera MVP).
  - Relevante para portfólio — demonstra experiência com cloud AWS.
- **Contras:**
  - Vendor lock-in com AWS (mitigado pelo uso de JWT padrão — trocar o `issuer-uri` é suficiente para migrar).
  - Customização de fluxos de autenticação é mais limitada que Keycloak.
  - Developer experience inferior ao Keycloak para cenários avançados (federation complexa, temas customizados).

### Keycloak (descartado)
- **Prós:** Flexibilidade total, UI de admin rica, sem vendor lock-in.
- **Contras:** Requer infraestrutura própria na AWS (ECS/Fargate), consome ~500MB+ de memória, complexidade operacional adicional (atualizações, backups, alta disponibilidade) sem ganho claro quando o deploy já é AWS.

### Auth0 (descartado)
- **Prós:** Setup rápido, boa DX.
- **Contras:** SaaS pago além do free tier (7.500 MAUs), vendor lock-in com terceiro fora do ecossistema AWS.

### Spring Authorization Server (descartado)
- **Prós:** 100% Spring nativo, controle total.
- **Contras:** Requer implementar manualmente telas de login, registro, recuperação de senha e gerenciamento de usuários — escopo excessivo para a V1.

## Implementação Prevista
1. Criar User Pool no AWS Cognito com atributos: email (obrigatório), name.
2. Configurar App Client no Cognito (OAuth2 Authorization Code flow + PKCE para frontend futuro).
3. Backend como **Resource Server**: validar JWT via `spring-boot-starter-oauth2-resource-server`.
4. Mapear o `sub` (Cognito user ID) para associar recursos ao usuário autenticado.
5. A entidade `User` do domínio mantém dados de negócio; credenciais ficam 100% no Cognito.
6. Para desenvolvimento local, usar **LocalStack** ou um mock de JWT para não depender de AWS.

## Consequências
- Endpoints protegidos por JWT; operações associadas ao usuário autenticado.
- Credenciais gerenciadas fora do backend (Cognito) — simplifica segurança.
- Possível necessidade de Cognito Lambda Triggers para lógica customizada (ex.: pós-registro).
- Migração futura facilitada: como o Spring valida JWT padrão, trocar Cognito por outro IdP requer apenas alterar o `issuer-uri`.

## Reversibilidade
Alta. O backend consome JWT padrão OIDC. Migrar para Keycloak, Auth0 ou outro IdP requer apenas reconfigurar o `issuer-uri` e ajustar claims, sem mudanças no código de domínio ou aplicação.
