# Configuração do AWS Cognito para GuitarGPT

Este guia descreve como configurar o AWS Cognito como provedor de identidade (IdP) para o GuitarGPT, implementando OAuth2 com JWT conforme especificado no [ADR-006](./adr/ADR-006-autenticacao-iam.md).

## Índice

1. [Pré-requisitos](#pré-requisitos)
2. [Criação do User Pool](#criação-do-user-pool)
3. [Configuração do App Client](#configuração-do-app-client)
4. [Variáveis de Ambiente](#variáveis-de-ambiente)
5. [Teste com cURL](#teste-com-curl)
6. [Integração Local (Desenvolvimento)](#integração-local-desenvolvimento)
7. [Deploy em Produção](#deploy-em-produção)
8. [Solução de Problemas](#solução-de-problemas)

---

## Pré-requisitos

- Conta AWS ativa com permissões para Cognito, IAM e Secrets Manager
- AWS CLI v2 instalado e configurado
- Java 21+ instalado localmente
- Docker e Docker Compose (para testes com container)
- cURL ou Postman (para testar endpoints)

---

## Criação do User Pool

### 1.1 Criar User Pool via AWS Console

1. Acesse [AWS Cognito Console](https://console.aws.amazon.com/cognito)
2. Clique em **"Create user pool"**
3. Escolha **"Cognito user pool"** (não federated identities)
4. Preencha as configurações:

#### Passo 1: Configure Sign-up Experience
- **Cognito user pool sign-in options**: Marque `Email`
- **User attribute permissions**: Deixe padrão
- Clique **Next**

#### Passo 2: Configure Security Requirements
- **Password policy**: Escolha `Custom`
  - Minimum length: `12`
  - Marque: Uppercase, Numbers, Special characters
- **Recommended MFA**: `No MFA` (para dev; em prod, use `Required`)
- Clique **Next**

#### Passo 3: Configure Sign-up Experience
- **Self-service sign-up**: Deixe desmarcado (apenas admin cria usuários para MVP)
- **Attribute verification and user account confirmation**: Deixe padrão
- Clique **Next**

#### Passo 4: Configure Message Delivery
- **Email provider**: Escolha `Send email with Cognito`
- Clique **Next**

#### Passo 5: Integrate Your App
- **User pool name**: `guitargpt-users`
- **App type**: `Public client`
- **App client name**: `guitargpt-app`
- **Client secret**: Deixe desmarcado (não use para SPAs/mobile)
- Clique **Next**

#### Passo 6: Review and Create
- Revise as configurações
- Clique **Create user pool**

Salve a URI: `https://cognito-idp.{region}.amazonaws.com/{user_pool_id}`

Exemplo: `https://cognito-idp.us-east-1.amazonaws.com/us-east-1_ABC123DEF`

---

## Configuração do App Client

### 2.1 Configurar OAuth2 Scopes e Redirect URIs

1. No User Pool criado, vá para **App integration** → **App clients and analytics**
2. Clique no app `guitargpt-app`
3. Na seção **App client settings**, configure:

#### Hosted UI
- **Enable token expiration**: ✅
- **Access token expiration**: `1` hour
- **Refresh token expiration**: `30` days
- **ID token expiration**: `1` hour

#### Authentication flows
- Marque:
  - `ALLOW_USER_PASSWORD_AUTH` (para testes)
  - `ALLOW_REFRESH_TOKEN_AUTH`
  - `ALLOW_USER_SRP_AUTH`

#### Allowed OAuth Flows
- Marque:
  - `Authorization code flow`
  - `Implicit flow` (apenas para SPAs)
  - `Client credentials flow` (se precisar de M2M)

#### Allowed OAuth Scopes
- `email`
- `openid`
- `profile`

#### Redirect URI (para testar com Swagger UI)
```
http://localhost:8080/login/oauth2/code/cognito
http://localhost:8080/swagger-ui.html
```

4. Clique **Save changes**

### 2.2 Obter Credenciais

Na página do App Client, copie e salve:
- **Client ID**
- **Client secret** (se habilitado; não será exibido novamente)
- **User pool ID** (ex: `us-east-1_ABC123DEF`)
- **Region** (ex: `us-east-1`)

---

## Variáveis de Ambiente

### 3.1 Arquivo `.env` (Desenvolvimento)

Crie `.env` na raiz do projeto:

```bash
# AWS Cognito
AWS_REGION=us-east-1
COGNITO_USER_POOL_ID=us-east-1_ABC123DEF
COGNITO_CLIENT_ID=1a2b3c4d5e6f7g8h9i0j
COGNITO_CLIENT_SECRET=your-secret-here

# Derivado - URI do Issuer
COGNITO_ISSUER_URI=https://cognito-idp.us-east-1.amazonaws.com/us-east-1_ABC123DEF

# App
APP_PORT=8080
SPRING_PROFILES_ACTIVE=docker
```

### 3.2 Docker Compose (`.env.docker`)

Para deploy com Docker:

```bash
COGNITO_ISSUER_URI=https://cognito-idp.us-east-1.amazonaws.com/us-east-1_ABC123DEF
POSTGRES_USER=guitargpt
POSTGRES_PASSWORD=guitargpt
DATABASE_URL=jdbc:postgresql://db:5432/guitargpt
```

### 3.3 AWS Secrets Manager (Produção)

```bash
# Armazenar secrets seguros em prod
aws secretsmanager create-secret \
  --name guitargpt/cognito \
  --secret-string '{
    "issuer_uri": "https://cognito-idp.us-east-1.amazonaws.com/us-east-1_ABC123DEF",
    "client_id": "...",
    "client_secret": "..."
  }'
```

---

## Teste com cURL

### 4.1 Criar Usuário de Teste (via AWS CLI)

```bash
aws cognito-idp admin-create-user \
  --user-pool-id us-east-1_ABC123DEF \
  --username testuser@example.com \
  --message-action SUPPRESS \
  --temporary-password TempPassword123! \
  --region us-east-1

# Definir senha permanente
aws cognito-idp admin-set-user-password \
  --user-pool-id us-east-1_ABC123DEF \
  --username testuser@example.com \
  --password Password123! \
  --permanent \
  --region us-east-1
```

### 4.2 Obter Token JWT

```bash
# 1. Autenticar e obter token
TOKEN=$(aws cognito-idp admin-initiate-auth \
  --user-pool-id us-east-1_ABC123DEF \
  --client-id 1a2b3c4d5e6f7g8h9i0j \
  --auth-flow ADMIN_USER_PASSWORD_AUTH \
  --auth-parameters USERNAME=testuser@example.com,PASSWORD=Password123! \
  --region us-east-1 \
  --query 'AuthenticationResult.AccessToken' \
  --output text)

echo "Access Token: $TOKEN"
```

### 4.3 Testar Endpoint Protegido

```bash
# Requisição COM token (deve retornar 200)
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/users

# Requisição SEM token (deve retornar 401)
curl http://localhost:8080/api/v1/users
```

### 4.4 Decodificar JWT (verificar claims)

```bash
# Instalar jwt-cli (opcional)
# npm install -g jwt-cli

jwt-cli decode $TOKEN

# Ou manualmente em https://jwt.io
```

---

## Integração Local (Desenvolvimento)

### 5.1 Perfil `dev` (Segurança Desativada)

Para desenvolvimento rápido sem Cognito:

```bash
# Executar com profile dev
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# Ou via Docker
SPRING_PROFILES_ACTIVE=dev docker-compose up -d
```

Neste perfil:
- ✅ Todos os endpoints são públicos (`permitAll()`)
- ✅ Nenhuma validação JWT
- ✅ Ideal para testes e prototipagem

### 5.2 Perfil `docker` (Com Cognito Real)

Para testar com Cognito real localmente:

```bash
# 1. Definir variável de ambiente
export COGNITO_ISSUER_URI="https://cognito-idp.us-east-1.amazonaws.com/us-east-1_ABC123DEF"

# 2. Iniciar container
SPRING_PROFILES_ACTIVE=docker docker-compose up -d

# 3. Verificar logs
docker-compose logs -f app

# 4. Testar
TOKEN=$(aws cognito-idp admin-initiate-auth ...)
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/users
```

### 5.3 Swagger UI com Suporte JWT

Com `SecurityConfig` em produção, o Swagger mostrará um ícone de cadeado:

1. Acesse http://localhost:8080/swagger-ui.html
2. Clique no ícone de cadeado
3. Cole o Access Token
4. Execute requests protegidos

---

## Deploy em Produção

### 6.1 Configurar em ECS / Lambda

```yaml
# task-definition.json
{
  "containerDefinitions": [
    {
      "name": "guitargpt",
      "image": "your-registry/guitargpt:latest",
      "environment": [
        {
          "name": "COGNITO_ISSUER_URI",
          "value": "https://cognito-idp.us-east-1.amazonaws.com/us-east-1_ABC123DEF"
        },
        {
          "name": "SPRING_PROFILES_ACTIVE",
          "value": "docker"
        }
      ]
    }
  ]
}
```

### 6.2 Application Load Balancer (ALB) com HTTPS

```bash
# Configurar redirect URI no Cognito
https://api.guitargpt.com/login/oauth2/code/cognito
https://api.guitargpt.com/swagger-ui.html
```

### 6.3 Monitoramento

```bash
# CloudWatch Logs
aws logs tail /ecs/guitargpt --follow

# Ver erros de autenticação
aws logs filter-log-events \
  --log-group-name /ecs/guitargpt \
  --filter-pattern "401\|unauthorized\|invalid_token"
```

---

## Solução de Problemas

### P1: "invalid_client" ao tentar obter token

**Causa**: Client ID ou Client Secret incorreto

**Solução**:
```bash
# Verificar credenciais
aws cognito-idp describe-user-pool-client \
  --user-pool-id us-east-1_ABC123DEF \
  --client-id 1a2b3c4d5e6f7g8h9i0j \
  --region us-east-1
```

### P2: "invalid_grant" ou "User does not exist"

**Causa**: Usuário não existe ou senha incorreta

**Solução**:
```bash
# Listar usuários
aws cognito-idp list-users \
  --user-pool-id us-east-1_ABC123DEF \
  --region us-east-1

# Resetar senha
aws cognito-idp admin-set-user-password \
  --user-pool-id us-east-1_ABC123DEF \
  --username testuser@example.com \
  --password NewPassword123! \
  --permanent \
  --region us-east-1
```

### P3: 401 Unauthorized em todos os endpoints

**Causa**: `COGNITO_ISSUER_URI` não configurada ou inválida

**Solução**:
```bash
# Verificar variável
echo $COGNITO_ISSUER_URI

# Validar URI
curl https://cognito-idp.us-east-1.amazonaws.com/us-east-1_ABC123DEF/.well-known/openid-configuration

# Logs da app
docker-compose logs app | grep -i "oauth\|cognito\|jwt"
```

### P4: Token expirado em meio à requisição

**Causa**: Token JWT expirou (TTL padrão: 1 hora)

**Solução**: Usar `refresh_token` para renovar:

```bash
NEW_TOKEN=$(aws cognito-idp admin-initiate-auth \
  --user-pool-id us-east-1_ABC123DEF \
  --client-id 1a2b3c4d5e6f7g8h9i0j \
  --auth-flow REFRESH_TOKEN_AUTH \
  --auth-parameters REFRESH_TOKEN=$REFRESH_TOKEN \
  --region us-east-1 \
  --query 'AuthenticationResult.AccessToken' \
  --output text)
```

### P5: CORS error ao chamar API desde frontend

**Causa**: `application.yml` não tem CORS configurado

**Solução**: Adicionar ao `application.yml`:

```yaml
spring:
  web:
    cors:
      allowed-origins: "https://app.guitargpt.com"
      allowed-methods: "GET,POST,PUT,DELETE"
      allowed-headers: "Authorization,Content-Type"
      allow-credentials: true
```

---

## Referências

- [AWS Cognito Documentation](https://docs.aws.amazon.com/cognito/)
- [Spring Security OAuth2 Resource Server](https://spring.io/projects/spring-security-oauth2-resource-server)
- [JWT.io](https://jwt.io) - Decodificador de tokens
- [ADR-006: Autenticação e IAM](./adr/ADR-006-autenticacao-iam.md)

---

## Checklist de Deploy

- [ ] User Pool criado em Cognito
- [ ] App Client configurado com OAuth2 scopes
- [ ] Redirect URIs adicionadas (dev + prod)
- [ ] `COGNITO_ISSUER_URI` definida em variáveis de ambiente
- [ ] Usuários de teste criados via CLI
- [ ] Token obtido com sucesso via `admin-initiate-auth`
- [ ] Endpoint protegido retorna 200 com token válido
- [ ] Endpoint protegido retorna 401 sem token
- [ ] Swagger UI mostra ícone de cadeado
- [ ] Logs da aplicação mostram validação JWT bem-sucedida
- [ ] (Prod) HTTPS configurado no ALB
- [ ] (Prod) CloudWatch Logs monitorando erros de auth
