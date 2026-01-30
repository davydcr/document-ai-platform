FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copiar pom.xml files (contexto é a raiz do projeto)
COPY backend/pom.xml ./
COPY backend/application/pom.xml ./application/
COPY backend/domain/pom.xml ./domain/
COPY backend/infrastructure/pom.xml ./infrastructure/

# Download dependencies
RUN mvn dependency:go-offline

# Copiar código fonte completo
COPY backend/ .

# Build
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copiar JAR gerado (arquivo correto - do infrastructure que tem a main class)
COPY --from=builder /build/infrastructure/target/infrastructure-*.jar app.jar

# Expor porta
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=5 \
    CMD wget --quiet --tries=1 --spider http://localhost:8080/api/health || exit 1

# Executar aplicação
ENTRYPOINT ["java", "-jar", "app.jar"]
