FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Copiar pom.xml
COPY backend/pom.xml ./
COPY backend/application/pom.xml ./application/
COPY backend/domain/pom.xml ./domain/
COPY backend/infrastructure/pom.xml ./infrastructure/

# Download dependencies
RUN mvn dependency:go-offline

# Copiar código
COPY backend/. .

# Build
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copiar JAR gerado
COPY --from=builder /build/application/target/document-ai-application-*.jar app.jar

# Expor porta
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=5 \
    CMD wget --quiet --tries=1 --spider http://localhost:8080/api/health || exit 1

# Executar aplicação
ENTRYPOINT ["java", "-jar", "app.jar"]
