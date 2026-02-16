# Configuração de Repositório (GitHub)

Este guia assume que o projeto **GuitarGPT** está hospedado no **GitHub**.

## 1) Branch principal
- Use `main` como branch padrão.
- Ative proteção de branch (Settings → Branches → Branch protection rules):
  - Require pull request reviews before merging (1 reviewer).
  - Require status checks to pass (selecione a verificação `build-test`).
  - Dismiss stale pull request approvals when new commits are pushed.

## 2) CI (GitHub Actions)
- Workflow criado em `.github/workflows/ci.yml`:
  - Compila e testa com **Java 21 (Temurin)** e **Maven**.
  - Publica artefatos de relatórios JUnit (útil para inspeção de falhas).
  - Em `push` na `main`, constrói e publica imagem Docker no **GitHub Container Registry (GHCR)**: `ghcr.io/<owner>/<repo>:<tag>`.

> Observação: o `GITHUB_TOKEN` já tem permissão para publicar no GHCR no mesmo repositório. Certifique-se de que o repositório é público ou que você tem acesso apropriado.

## 3) Docker
- Garanta um `Dockerfile` na raiz do repo. Exemplo mínimo (Java 21 + JAR único):
  ```dockerfile
  FROM eclipse-temurin:21-jre
  WORKDIR /app
  COPY target/*.jar app.jar
  EXPOSE 8080
  ENTRYPOINT ["java","-jar","/app/app.jar"]
  ```

## 4) Versão do Java
- Arquivos de build:
  - `pom.xml` com `maven-compiler-plugin` target/source 21 e `spring-boot-maven-plugin`.
  - Defina `java.version` em `properties` como `21`.

## 5) Próximos passos sugeridos
- Adicionar **codeowners** (`.github/CODEOWNERS`) para revisão padrão.
- Criar **templates de Issue/PR** (`.github/ISSUE_TEMPLATE/*.md`, `.github/PULL_REQUEST_TEMPLATE.md`).
- Publicar documentação gerada com **springdoc-openapi** e instrução para execução local via **Docker Compose** (app + PostgreSQL + Kafka/Redpanda).
