# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

# Copia o pom.xml e baixa as dependências (cache layer separada)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copia o código fonte e compila
COPY src ./src
RUN mvn clean package -DskipTests -B

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Cria usuário não-root por segurança
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

# Cria diretório de uploads com permissões corretas
RUN mkdir -p /app/uploads/videos /app/uploads/images \
    && chown -R appuser:appgroup /app

# Copia o JAR gerado no stage de build
COPY --from=build /app/target/*.jar app.jar

USER appuser

# Variáveis de ambiente com valores padrão
ENV JAVA_OPTS="-Xms256m -Xmx512m" \
    SERVER_PORT=8080 \
    STORAGE_LOCAL_PATH=/app/uploads

EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]