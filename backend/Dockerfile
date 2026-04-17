# ---------- Build stage ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Copy Maven wrapper and pom first to leverage Docker layer caching
COPY mvnw* pom.xml ./
COPY .mvn .mvn

# Resolve dependencies (will cache if pom.xml unchanged)
RUN ./mvnw -B -q -Dmaven.test.skip=true dependency:go-offline

# Copy sources
COPY src src

# Build (skip tests in image build, CI já testa)
RUN ./mvnw -B -DskipTests package

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Defina o timezone via variável se necessário: -e TZ=America/Sao_Paulo
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Dfile.encoding=UTF-8"
ENV SPRING_PROFILES_ACTIVE=prod

# Copia o jar final (ajuste se o nome for diferente)
COPY --from=build /workspace/target/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
